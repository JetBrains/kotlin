# Шаг 1. Ревизия существующих реализаций BitSet в экосистеме Kotlin

## Резюме

Проведена ревизия API surface шести BitSet-реализаций: `java.util.BitSet` (baseline), `kotlin.native.BitSet` (публичный, `@ObsoleteNativeApi`), внутренний Wasm BitSet (урезанная копия для regex), `CustomBitSet` (оптимизация для devirtualization в K/N compiler), а также две сторонние Kotlin-библиотеки (BitVector, KmpIO). Отдельно рассмотрены extension-утилиты `BitSetUtil.kt` для `java.util.BitSet`. Большинство реализаций мутабельные; BitVector — единственная библиотека с разделением на read-only base type и mutable subtype (sealed `BitVector` + `MutableBitVector`), хотя read-only гарантия обеспечивается только на уровне типовой системы и ослаблена public getter'ом `words: IntArray`. Внутреннее представление — массив слов (LongArray или IntArray). Из рассмотренных реализаций только BitVector реализует `Iterable<Int>`. Ключевые пробелы `kotlin.native.BitSet` по сравнению с Java: отсутствие `cardinality()`, factory methods, конвертации в byte/long массивы и stream-подобной итерации; при этом он добавляет Kotlin-идиомы (IntRange, operator get, initializer-конструктор).

---

## 1. java.util.BitSet (baseline)

**Пакет:** `java.util`
**Тип:** `public class BitSet implements Cloneable, Serializable`
**Мутабельность:** мутабельный
**Размер:** динамический (автоматически растёт)
**Внутреннее представление:** `long[] words`

### Полный API

#### Конструкторы и фабрики

| Сигнатура | Описание |
|---|---|
| `BitSet()` | Пустой BitSet, начальная ёмкость 64 бита |
| `BitSet(int nbits)` | Пустой BitSet с указанной начальной ёмкостью |
| `static BitSet valueOf(long[])` | Создание из массива long |
| `static BitSet valueOf(LongBuffer)` | Создание из LongBuffer |
| `static BitSet valueOf(byte[])` | Создание из массива byte |
| `static BitSet valueOf(ByteBuffer)` | Создание из ByteBuffer |

#### Мутация (установка/очистка/инверсия)

| Сигнатура | Описание |
|---|---|
| `void set(int bitIndex)` | Установить бит в `true` |
| `void set(int bitIndex, boolean value)` | Установить бит в указанное значение |
| `void set(int fromIndex, int toIndex)` | Установить диапазон `[from, to)` в `true` |
| `void set(int fromIndex, int toIndex, boolean value)` | Установить диапазон в указанное значение |
| `void clear()` | Очистить все биты |
| `void clear(int bitIndex)` | Очистить один бит |
| `void clear(int fromIndex, int toIndex)` | Очистить диапазон `[from, to)` |
| `void flip(int bitIndex)` | Инвертировать один бит |
| `void flip(int fromIndex, int toIndex)` | Инвертировать диапазон `[from, to)` |

#### Запросы (чтение состояния)

| Сигнатура | Описание |
|---|---|
| `boolean get(int bitIndex)` | Значение одного бита |
| `BitSet get(int fromIndex, int toIndex)` | Новый BitSet — подмножество из диапазона `[from, to)` |
| `int length()` | Индекс старшего set-бита + 1 (или 0, если пустой) |
| `int size()` | Текущая ёмкость в битах (выровнена по `long`) |
| `int cardinality()` | Количество установленных битов |
| `boolean isEmpty()` | `true`, если нет установленных битов |

#### Навигация по битам

| Сигнатура | Описание |
|---|---|
| `int nextSetBit(int fromIndex)` | Следующий set-бит >= `fromIndex`; `-1`, если нет |
| `int nextClearBit(int fromIndex)` | Следующий clear-бит >= `fromIndex` |
| `int previousSetBit(int fromIndex)` | Предыдущий set-бит <= `fromIndex`; `-1`, если нет |
| `int previousClearBit(int fromIndex)` | Предыдущий clear-бит <= `fromIndex`; `-1`, если нет |

#### Bulk-операции (побитовые, in-place)

| Сигнатура | Описание |
|---|---|
| `void and(BitSet set)` | `this &= set` |
| `void or(BitSet set)` | `this \|= set` |
| `void xor(BitSet set)` | `this ^= set` |
| `void andNot(BitSet set)` | `this &= ~set` |
| `boolean intersects(BitSet set)` | `true`, если есть общие set-биты |

#### Конвертация

| Сигнатура | Описание |
|---|---|
| `byte[] toByteArray()` | Байтовое представление (little-endian в пределах слова) |
| `long[] toLongArray()` | Long-представление |

#### Stream API (Java 8+)

| Сигнатура | Описание |
|---|---|
| `IntStream stream()` | Stream индексов установленных битов |

#### Объектные методы

| Сигнатура | Описание |
|---|---|
| `boolean equals(Object)` | Поэлементное сравнение по значению (trailing zeros игнорируются) |
| `int hashCode()` | Хэш на основе содержимого |
| `Object clone()` | Deep copy (клонирует внутренний `long[]`) |
| `String toString()` | Формат `{0, 2, 5}` (индексы set-битов через `, `, в фигурных скобках) |

### Семантика size/length/cardinality

Три разных понятия «размера» — частый источник путаницы:

| Метод | Что возвращает | Пример для BitSet с битами {0, 5, 100} |
|---|---|---|
| `size()` | Ёмкость в битах (кратна 64) | 128 (2 long слова) |
| `length()` | Индекс старшего set-бита + 1 | 101 |
| `cardinality()` | Количество set-битов | 3 |

### Ключевые характеристики

- **Не реализует `Collection`/`Set`/`Iterable`** — итерация только через `nextSetBit()` цикл или `stream()`.
- **Только мутабельный** — нет immutable-варианта.
- **Диапазоны полуоткрытые** — `[from, to)`, как в стандартных Java-коллекциях.
- **Negative index → `IndexOutOfBoundsException`** (исключение: `previousSetBit(-1)` и `previousClearBit(-1)` валидны и возвращают `-1`; индексы < -1 бросают исключение).
- **`get(from, to)`** возвращает новый BitSet — единственная операция, возвращающая копию (кроме `clone()`).
- **Thread safety** — не thread-safe; документация явно указывает на необходимость внешней синхронизации.

---

## 2. kotlin.native.BitSet

**Файл:** `kotlin-native/runtime/src/main/kotlin/kotlin/native/BitSet.kt`
**Пакет:** `kotlin.native`
**Тип:** `public actual class BitSet`
**Аннотации:** `@ObsoleteNativeApi` (API считается устаревшим и выводится из употребления; требует opt-in на уровне `ERROR`, subject to removal in a future release)
**Мутабельность:** мутабельный
**Размер:** динамический (автоматически растёт)
**Внутреннее представление:** `LongArray` (64 бита на слово)
**Интерфейсы:** не реализует (ни `Collection`, ни `Iterable`, ни `Serializable`)

### API surface

#### Конструкторы

| Сигнатура | Описание |
|---|---|
| `BitSet(size: Int = 64)` | Пустой BitSet с указанным начальным размером |
| `BitSet(length: Int, initializer: (Int) -> Boolean)` | BitSet заданной длины, заполненный через лямбду |

#### Свойства

| Сигнатура | Описание |
|---|---|
| `val isEmpty: Boolean` | `true`, если нет set-битов |
| `var size: Int` (private set) | Текущее количество доступных битов |
| `val lastTrueIndex: Int` | Индекс последнего set-бита; `-1`, если пустой |

#### Мутация

| Сигнатура | Описание |
|---|---|
| `fun set(index: Int, value: Boolean = true)` | Установить бит |
| `fun set(from: Int, to: Int, value: Boolean = true)` | Установить диапазон `[from, to)` |
| `fun set(range: IntRange, value: Boolean = true)` | Установить inclusive-диапазон |
| `fun clear(index: Int)` | Очистить один бит (делегирует `set(index, false)`) |
| `fun clear(from: Int, to: Int)` | Очистить диапазон (делегирует `set(from, to, false)`) |
| `fun clear(range: IntRange)` | Очистить inclusive-диапазон (делегирует `set(range, false)`) |
| `fun clear()` | Очистить все биты |
| `fun flip(index: Int)` | Инвертировать один бит |
| `fun flip(from: Int, to: Int)` | Инвертировать диапазон `[from, to)` |
| `fun flip(range: IntRange)` | Инвертировать inclusive-диапазон |

#### Запросы

| Сигнатура | Описание |
|---|---|
| `operator fun get(index: Int): Boolean` | Значение бита (оператор `[]`) |

#### Навигация

| Сигнатура | Описание |
|---|---|
| `fun nextSetBit(startIndex: Int = 0): Int` | Следующий set-бит; `-1`, если нет |
| `fun nextClearBit(startIndex: Int = 0): Int` | Следующий clear-бит; `startIndex`, если `startIndex >= size` |
| `fun previousSetBit(startIndex: Int): Int` | Предыдущий set-бит; `-1`, если нет. `previousSetBit(-1)` валиден — возвращает `-1` |
| `fun previousClearBit(startIndex: Int): Int` | Предыдущий clear-бит; `-1`, если нет. `previousClearBit(-1)` валиден — возвращает `-1` |

#### Bulk-операции (in-place)

| Сигнатура | Описание |
|---|---|
| `fun and(another: BitSet)` | `this &= another` |
| `fun or(another: BitSet)` | `this \|= another` |
| `fun xor(another: BitSet)` | `this ^= another` |
| `fun andNot(another: BitSet)` | `this &= ~another` |
| `fun intersects(another: BitSet): Boolean` | Есть ли общие set-биты |

#### Объектные методы

| Сигнатура | Описание |
|---|---|
| `toString(): String` | Формат `[0\|5\|6\|11]` (через `\|`, в квадратных скобках) |
| `hashCode(): Int` | Хэш на основе содержимого (формула как в Java) |
| `equals(other: Any?): Boolean` | Поэлементное сравнение (trailing zeros игнорируются, как в Java) |

### Дизайн-решения и отличия от java.util.BitSet

| Аспект | java.util.BitSet | kotlin.native.BitSet | Комментарий |
|---|---|---|---|
| Operator `[]` | нет | `operator fun get` | Kotlin-идиома |
| IntRange перегрузки | нет | `set(range)`, `clear(range)`, `flip(range)` | Kotlin-идиома |
| Initializer-конструктор | нет | `BitSet(length) { ... }` | Удобно для создания с предикатом |
| `lastTrueIndex` | нет (есть `length() - 1`) | свойство | Более наглядно |
| `cardinality()` | есть | **нет** | Значимый пробел |
| `clone()` / `copy()` | `clone()` | **нет** | Нет способа скопировать |
| Factory `valueOf()` | 4 перегрузки | **нет** | Нет создания из массива |
| `toByteArray()`/`toLongArray()` | есть | **нет** | Нет конвертации |
| `get(from, to)` → BitSet | есть | **нет** | Нет подмножества |
| `stream()` / итерация | `stream()` (Java 8+) | **нет** | Итерация только через `nextSetBit` цикл |
| `size()` семантика | ёмкость (кратна 64) | кол-во доступных битов | Разная семантика! Кроме того, `clear(index)` / `clear(from, to)` / `clear(range)` за пределами `size` расширяют его (делегируют в `set(..., false)` → `ensureCapacity()`); parameterless `clear()` только обнуляет массив без изменения `size`. В Java `clear(index)` за пределами `wordsInUse` — no-op |
| `length()` | idx старшего бита + 1 | нет аналога (есть `lastTrueIndex + 1`) | — |
| `toString()` формат | `{0, 2, 5}` | `[0\|2\|5]` | Разные разделители и скобки |
| Диапазоны `set/clear/flip` | `[from, to)` полуоткрытые; `from > to` → `IndexOutOfBoundsException` | `[from, to)` + `IntRange` (inclusive); `from > to` → silent no-op (через `from until to` → пустой range) | Обе семантики, но reversed-range поведение расходится |
| Ошибки при отрицательных индексах | `IndexOutOfBoundsException` (кроме `previous*Bit(-1)` — валиден, возвращает `-1`) | `IndexOutOfBoundsException` (кроме `previous*Bit(-1)` — валиден, возвращает `-1`) | Поведение идентично: оба допускают `-1` для `previous*` методов |

### Известные проблемы в коде

1. **TODO-комментарий** в `ensureCapacity()`: `"Set all bits after the index to 0. TODO: We can remove it."` — `clearUnusedTail()` вызывается при каждом расширении, что может быть избыточным.
2. **Неоптимальная реализация `nextBit()`** — линейный поиск по отдельным битам (`for (offset in startOffset..MAX_BIT_OFFSET)`), в то время как Java использует `Long.numberOfTrailingZeros()` для пропуска нулевых слов целиком.
3. **Неоптимальная реализация `getMaskBetween()`** — цикл вместо битовых операций. Java вычисляет маски как `WORD_MASK << fromIndex` и `WORD_MASK >>> -toIndex`.
4. **Баг в `flip(range: IntRange)`** (строка 220): `flipBitsWithMask(toIndex, toOffset.asMaskAfter)` — инвертирует bits[toIndex], но должно быть `fromIndex` и `fromOffset.asMaskAfter` для первого элемента (copy-paste с set).
5. **`@ObsoleteNativeApi`** — API считается obsolete и выводится из употребления ("being phased out"). Аннотация использует `@RequiresOptIn` на уровне `ERROR` с сообщением "subject to removal in a future release". Пользователи могут уже от него зависеть.

---

## 3. Wasm internal BitSet

**Файл:** `libraries/stdlib/wasm/src/kotlin/text/regex/BitSet.kt`
**Пакет:** `kotlin.native` (исторически; комментарий: "stripped copy of K/N implementation for Regex")
**Видимость:** `internal actual class`
**Аннотации:** `@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")`

### API surface

Урезанный subset Native-реализации. API surface определяется shared expect-декларацией из `libraries/stdlib/native-wasm/src/kotlin/native/BitSet.kt`, которую wasm-actual реализует.

| Что есть | Что отсутствует (по сравнению с Native runtime) |
|---|---|
| `isEmpty`, `size` | `lastTrueIndex` |
| `set(index)`, `set(from, to)`, `set(range)` | `clear(index)`, `clear(from, to)`, `clear(range)` |
| `get(index)` (operator) | `flip(index)`, `flip(from, to)`, `flip(range)` |
| `nextSetBit()`, `nextClearBit()` | `previousSetBit()`, `previousClearBit()` |
| `and()`, `or()`, `xor()`, `andNot()`, `intersects()` | — |
| — | `toString()`, `hashCode()`, `equals()` |

### Назначение

Используется исключительно как внутренняя структура данных для Wasm regex-движка. Урезанный набор API определяется общей native-wasm expect/actual формой (shared expect-декларация в `native-wasm/src/kotlin/native/BitSet.kt`). Regex-код активно использует не только установку бит и навигацию вперёд, но и bulk-операции: `and`, `or`, `xor`, `andNot` (в `CharClass.union()`, `CharClass.intersection()`, `CharClass.add()`) и `intersects` (в `AbstractCharClass`).

### Вывод для нового API

Кандидат на миграцию к общей multiplatform-реализации. Текущий internal-статус означает, что ломающие изменения не затронут внешних пользователей.

---

## 4. Внутренние реализации в репозитории Kotlin

### 4.1 CustomBitSet (compiler, devirtualization)

**Файл:** `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/util/CustomBitSet.kt`
**Видимость:** `internal class`
**Назначение:** оптимизированный BitSet для dataflow analysis в K/N backend (devirtualization)

#### API surface

| Сигнатура | Описание | Есть ли аналог в java.util.BitSet |
|---|---|---|
| `CustomBitSet()` | Пустой | `BitSet()` |
| `CustomBitSet(nodesCount: Int)` | С ёмкостью | `BitSet(int)` |
| `valueOf(data: LongArray): CustomBitSet` | Фабрика из LongArray | `BitSet.valueOf(long[])` |
| `set(bitIndex: Int)` | Установить бит | `set(int)` |
| `clear(bitIndex: Int)` | Очистить бит (вызывает `ensureCapacity` — может расширить `size`) | `clear(int)` |
| `operator fun get(bitIndex: Int): Boolean` | Читать бит | `get(int)` |
| `operator fun set(bitIndex: Int, value: Boolean)` | Установить с bool | `set(int, boolean)` |
| `cardinality(): Int` | Кол-во set-битов | `cardinality()` |
| **`forEachBit(block: (Int) -> Unit)`** | Итерация по set-битам | **нет** (только `nextSetBit` цикл или `stream()`) |
| **`forEachWord(block: (Long) -> Unit)`** | Итерация по raw-словам | **нет** |
| `clear()` | Очистить все | `clear()` |
| `or(another)` | in-place OR | `or(BitSet)` |
| **`orWithFilterHasChanged(another): Boolean`** | OR с детекцией изменений | **нет** |
| **`orWithFilterHasChanged(another, filter): Boolean`** | Filtered OR с детекцией | **нет** |
| `and(another)` | in-place AND | `and(BitSet)` |
| `andNot(another)` | in-place AND NOT | `andNot(BitSet)` |
| `intersects(another): Boolean` | Проверка пересечения | `intersects(BitSet)` |
| **`contains(another): Boolean`** | Проверка подмножества (`operator`) | **нет** |
| **`copy(): CustomBitSet`** | Полная копия | `clone()` |
| `equals(other)` / `hashCode()` | Сравнение по значению (сначала сравнивает `size` — два BitSet с одинаковыми set-битами, но разным внутренним размером считаются неравными; отличие от `java.util.BitSet`, который игнорирует trailing zeros). Поскольку `clear(bitIndex)` и `operator set(_, false)` вызывают `ensureCapacity` (parameterless `clear()` делает обратное: обнуляет `data` и сбрасывает `size = 0`), noop-подобная очистка (`clear(1000)` на пустом наборе) меняет `size` и, как следствие, `equals`/`hashCode` | `equals()` / `hashCode()` |
| **`hashCodeLong(): Long`** | 64-битный хэш | **нет** |

#### Уникальные решения (не в java.util.BitSet)

1. **`forEachBit()`** — высокопроизводительная итерация через `countTrailingZeroBits()` (bit-twiddling hack `d and -d`). Это самый частый паттерн использования — `BitSetUtil.kt` добавляет ту же функцию как extension для `java.util.BitSet`.
2. **`orWithFilterHasChanged()`** — операция OR, возвращающая boolean "изменились ли данные". Критична для алгоритмов с фиксированной точкой (dataflow analysis), где нужно знать, когда итерации можно остановить.
3. **`contains(another)`** — проверка подмножества (`another ⊆ this`). Отсутствует в java.util.BitSet; в dataflow analysis используется для проверки, что одно множество включает другое.
4. **`operator fun set(bitIndex, value)`** — в отличие от `kotlin.native.BitSet`, поддерживает `bitSet[i] = true` синтаксис (operator set).

### 4.2 BitSetUtil.kt (extensions для java.util.BitSet)

**Файл:** `compiler/util/src/org/jetbrains/kotlin/utils/BitSetUtil.kt`

| Сигнатура | Описание | Зачем нужно |
|---|---|---|
| `fun BitSet.copy(): BitSet` | Копирование через `BitSet(this.size()).apply { this.or(this@copy) }` | `clone()` возвращает `Object`, неудобно |
| `inline fun BitSet.forEachBit(block: (Int) -> Unit)` | Итерация по set-битам через `nextSetBit` | Цикл `while (nextSetBit(...) >= 0)` — boilerplate |
| `inline fun <R> BitSet.mapEachBit(block: (Int) -> R): List<R>` | Map по set-битам | То же, но с результатом |

#### Что эти extensions компенсируют

- **`copy()`** — `clone()` в Java возвращает `Object`, требует каст. Kotlin-расширение даёт типизированную копию.
- **`forEachBit()`** — канонический паттерн итерации в Java (`while ((i = nextSetBit(i + 1)) >= 0) { ... }`) — многословный и error-prone (легко забыть `+1` или проверку `>= 0`). Extension инкапсулирует этот паттерн.
- **`mapEachBit()`** — дальнейшее расширение: collect результатов итерации в `List<R>`.

---

## 5. Сторонние Kotlin-библиотеки

### 5.1 BitVector (adokky/bitvector)

**Репозиторий:** [github.com/adokky/bitvector](https://github.com/adokky/bitvector)
**Источник:** исходный код ветки `main`, commit [`a70c176`](https://github.com/adokky/bitvector/commit/a70c176d8d5772579fc99bd0491f8be05f2ae273). API dump в репозитории (`bitvector-core.api`) не синхронизирован с текущими исходниками. В dump bulk-операции на `MutableBitVector` экспортируются как `and`/`or`/`xor`/`andNot` (in-place), тогда как в исходниках in-place варианты переименованы в `mutateAnd`/`mutateOr`/`mutateXor`/`mutateAndNot`; при этом `and`/`or`/`xor` (new-returning) по-прежнему существуют на базовом `BitVector`. За основу взяты исходники.
**Платформы:** Kotlin Multiplatform (JVM, JS, Wasm, Native — iOS, macOS, Linux, Windows, tvOS, watchOS, Android Native)
**Мутабельность:** read-only `BitVector` (sealed base type) + mutable `MutableBitVector`
**Внутреннее представление:** `IntArray` (32 бита на слово)
**Интерфейсы:** `BitVector` реализует `Iterable<Int>` (через `IntIterator`)

#### API surface

**BitVector (read-only sealed base type):**

| Категория | Сигнатура | Описание |
|---|---|---|
| Фабрики | `BitVector.Empty: BitVector` (companion property, `@JvmName("empty")`) | Пустой BitVector |
| | `bitsOf(vararg Int): BitVector` | Создание из индексов set-битов |
| Запросы | `get(index): Boolean` | Значение бита |
| | `contains(index: Int): Boolean` | `index in bv` — operator |
| | `contains(other: BitVector): Boolean` | Проверка подмножества |
| | `cardinality(): Int` | Количество set-битов |
| | `isEmpty(): Boolean` | Нет set-битов? |
| | `length(): Int` | Индекс старшего set-бита + 1 |
| | `intersects(other: BitVector): Boolean` | Есть ли общие set-биты |
| Навигация | `first(lookingForTrue: Boolean): Int` | Первый set/clear бит |
| | `first(from, to, lookingForTrue): Int` | Первый set/clear бит в диапазоне |
| | `last(): Int` | Последний set-бит |
| Итерация | `iterator(): IntIterator` | Итератор по индексам set-битов (`Iterable`) |
| | `forEachBit { }` | Inline-итерация по set-битам |
| | `forEachZeroBit { }` | Inline-итерация по clear-битам |
| | `forEachBitBreakable { }` | Итерация с возможностью прерывания |
| Копирование | `copy(): BitVector` | Независимая копия |
| Внутреннее | `words: IntArray` (свойство, public getter / `protected set`) | Доступ к raw-представлению; содержимое массива можно мутировать извне |
| Объектные | `equals()`, `hashCode()`, `toString()` | `equals` и `toString` корректны; `hashCode()` содержит баг: для `BitVector.Empty` (`IntArray(0)`) обращение к `words[0]` → `ArrayIndexOutOfBoundsException` (цикл `while (word >= i)` выполняется при `length() == 0` → `word == 0`) |

**MutableBitVector (extends BitVector):**

| Категория | Сигнатура | Описание |
|---|---|---|
| Конструкторы | `MutableBitVector(capacity: Int)` | С начальной ёмкостью (округляет вниз по 32-битным словам: `IntArray((capacity / 32).coerceAtLeast(1))`; `MutableBitVector(33)` выделяет только 32 бита) |
| | `MutableBitVector(source: BitVector)` | Копирование из существующего |
| Фабрики | `mutableBitsOf(vararg Int): MutableBitVector` | Создание из индексов |
| | `MutableBitVector.wrap(IntArray): MutableBitVector` | Обёртка над массивом |
| Мутация | `set(index)`, `set(index, Boolean)` | Установить бит |
| | `put(index): Boolean` | Установить и вернуть старое значение |
| | `unset(index)` | Очистить бит |
| | `clear()` | Очистить все биты |
| | `clear(from, to)`, `clear(IntRange)` | Очистить диапазон |
| | `flip(index)` | Инвертировать один бит |
| | `invert(nbits)` | Инвертировать первые `nbits` бит |
| | `fill(IntRange)` | Установить диапазон в `true` |
| Bulk-операции (new) | `and(BitVector)`, `or(BitVector)`, `xor(BitVector)` | Определены на базовом `BitVector`; **всегда** возвращают **новый** `MutableBitVector`, типизированный как `BitVector` (даже при вызове на `MutableBitVector`) |
| Bulk-операции (in-place) | `mutateAnd(BitVector)`, `mutateOr(BitVector)`, `mutateXor(BitVector)`, `mutateAndNot(BitVector)` | Определены только на `MutableBitVector`; модифицируют `this` in-place. `andNot` доступен **только** как `mutateAndNot` — отдельного new-returning `andNot` нет |
| Ёмкость | `ensureCapacity(Int)` | Гарантировать минимальную ёмкость |
| Копирование | `copy(): MutableBitVector` | Независимая мутабельная копия |

#### Ключевые решения

| Решение | Описание |
|---|---|
| **Read-only/Mutable разделение** | `BitVector` — sealed read-only base type, `MutableBitVector` — единственная concrete-реализация. Фабрика `bitsOf()` фактически возвращает `MutableBitVector`, типизированный как `BitVector`. Immutability обеспечивается на уровне типовой системы (отсутствие мутирующих методов в `BitVector`), а не отдельной неизменяемой реализацией. Bulk-операции `and`/`or`/`xor` на `BitVector` также возвращают `MutableBitVector`. Дополнительно, `words: IntArray` имеет public getter, что позволяет мутировать содержимое массива извне без downcasting'а (`bv.words[i] = …`), ослабляя read-only гарантию ещё больше. |
| **`Iterable<Int>`** | `BitVector` реализует `Iterable`, позволяя использовать `for (bit in bv)`, `filter`, `map` и т.д. |
| **IntArray вместо LongArray** | Осознанный выбор: на JS `Long` эмулируется, `Int` — нативный. Производительность на JS предположительно выше (гипотеза, требует проверки бенчмарками в шаге 10). |
| **Навигация через `first()`/`last()`** | Частичный аналог `nextSetBit`/`nextClearBit` — `first(from, to, lookingForTrue)`. `last()` возвращает глобальный последний set-бит (без параметра `from`), т.е. **не** является аналогом `previousSetBit(from)`. |
| **`contains(BitVector)`** | Проверка подмножества — аналог `CustomBitSet.contains()`. |

#### Отличия от java.util.BitSet

- Нет `previousSetBit()`/`previousClearBit()` — обратная навигация отсутствует (`last()` глобальный, без параметра `from`).
- Нет `valueOf(LongArray/ByteArray)` (но есть `bitsOf()`, `mutableBitsOf()`, `wrap()`).
- Нет конвертации в byte/long массивы (только свойство `words: IntArray`).
- Нет `set(from, to)` диапазонов для установки (но есть `fill(IntRange)` и `clear(from, to)`/`clear(IntRange)`).
- Есть read-only base type (Java — только мутабельный).
- Есть `Iterable<Int>` (Java — только `stream()`).

### 5.2 KmpIO (skolson/KmpIO)

**Репозиторий:** [github.com/skolson/KmpIO](https://github.com/skolson/KmpIO), commit [`6015ebe`](https://github.com/skolson/KmpIO/commit/6015ebe5a5132fa0483a3ee9de778a772e726324)
**Артефакт:** `io.github.skolson:kmp-io:0.3.0`
**Платформы:** JVM, Android, macOS (x64, arm64), iOS (x64, arm64, simulator arm64), Linux (x64, arm64)
**Мутабельность:** мутабельный
**Размер:** смешанная семантика — внутренний `words` расширяется автоматически, но конструкторный `numberOfBits` используется как логическая граница: `toByteArray()` рассчитывает размер результата из `numberOfBits`, а `iterateClearBits()` останавливается на `numberOfBits`
**Внутреннее представление:** `LongArray` (64 бита на слово)

#### API surface

| Категория | Сигнатура | Описание |
|---|---|---|
| Конструкторы | `BitSet(numberOfBits: Int)` | Пустой BitSet с заданной ёмкостью |
| | `BitSet(bytes: ByteArray, bitsCount: Int)` | Создание из байтового массива |
| | `BitSet(buffer: ByteBuffer, bitsCount: Int)` | Создание из ByteBuffer |
| Фабрики | `create(longs: LongArray): BitSet` | Создание из LongArray (companion object; убирает trailing zeros). *Баг: `numberOfBits` остаётся 0 — `toByteArray()` и `iterateClearBits()` не работают корректно для экземпляров, созданных через эту фабрику* |
| Мутация | `set(bitIndex, on: Boolean)` | Установить/очистить бит |
| | `clear(bitIndex)` | Очистить один бит |
| | `clear()` | Очистить все биты (*баг: `0..words.size` — inclusive-range, AIOOBE при вызове*) |
| | `flip(bitIndex)` | Инвертировать один бит |
| | `flip(fromIndex, toIndex)` | Инвертировать диапазон |
| Запросы | `get(bitIndex): Boolean` | Значение бита |
| | `size(): Int` | Ёмкость в битах |
| | `length: Int` (свойство) | Индекс старшего set-бита + 1 |
| | `empty: Boolean` (свойство) | Нет set-битов? |
| Навигация | `nextSetBit(fromIndex): Int` | Следующий set-бит |
| | `nextClearBit(fromIndex): Int` | Следующий clear-бит |
| Итерация | `iterateSetBits(startIndex, onSetBit: (Int) -> Boolean): Int` | Callback-итерация по set-битам |
| | `iterateClearBits(startIndex, onClearedBit: (Int) -> Boolean): Int` | Callback-итерация по clear-битам |
| Конвертация | `toByteArray(): ByteArray` | Байтовое представление (little-endian) |
| | `toString(): String` | Строка вида `", 1, 4, 8"` (без скобок, через `", "`; не более 51 set-бита — off-by-one: `count++` — post-increment, поэтому проверка `50 < 50` возвращает `false` уже после добавления 51-го элемента в строку) |

#### Ключевые решения

| Решение | Описание |
|---|---|
| Pure Kotlin | Единый код для всех платформ, без expect/actual |
| Близость к java.util.BitSet | API покрывает большую часть Java BitSet: навигация, flip, clear (*баг — см. API table*), toByteArray, toString |
| Bracket notation | `bitSet[index]` для чтения и записи |
| ByteArray/ByteBuffer-конструкторы | Создание из байтового массива или буфера (для бинарных протоколов) |
| Callback-итерация | `iterateSetBits`/`iterateClearBits` — итерация с возможностью прерывания через boolean return |

#### Отличия от java.util.BitSet

- Нет bulk bitwise-операций (`and`/`or`/`xor`/`andNot`).
- Нет `intersects()`, `cardinality()`.
- Нет `previousSetBit()`/`previousClearBit()`.
- Нет `equals()`/`hashCode()`.
- Нет default-конструктора (требуется `numberOfBits`).
- Есть callback-итерация `iterateSetBits`/`iterateClearBits` (нет в Java).

---

## 6. Сравнительная таблица API surfaces

Легенда: **+** = есть, **-** = нет, **~** = частичный аналог (другой API), **(ext)** = через extension function, **(op)** = operator overload, **(new)** = возвращает новый объект (не in-place). Колонка **BitVector (lib)** агрегирует API обоих типов библиотеки: `BitVector` (read-only sealed base type) и `MutableBitVector` (mutable concrete subtype).

### Конструкторы и фабрики

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| Default constructor | + | + (size=64) | + (size=64) | + | + | - |
| Capacity constructor | + | + | + | + | + (округляет вниз) | + |
| Initializer lambda | - | + | - | - | - | - |
| `valueOf(LongArray)` | + | - | - | + | - | ~ (`create`; баг: `numberOfBits=0`) |
| `valueOf(ByteArray)` | + | - | - | - | - | + |
| `valueOf(Buffer)` | + | - | - | - | - | + (`ByteBuffer`) |
| `bitsOf(vararg)` фабрика | - | - | - | - | + | - |

### Мутация (одиночные биты)

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `set(index)` | + | + | + | + | + | - |
| `set(index, bool)` | + | + | + | + (op) | + | + (op) |
| `clear(index)` | + | + | - | + | + (`unset`) | + |
| `flip(index)` | + | + | - | - | + | + |

### Мутация (диапазоны)

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `set(from, to)` | + `[from,to)` | + `[from,to)` | + `[from,to)` | - | - (`fill(IntRange)`) | - |
| `set(IntRange)` | - | + (inclusive) | + (inclusive) | - | + (`fill`) | - |
| `clear(from, to)` | + | + | - | - | + | - |
| `clear(IntRange)` | - | + | - | - | + | - |
| `flip(from, to)` | + | + | - | - | - | + |
| `flip(IntRange)` | - | + | - | - | - | - |
| `clear()` (все биты) | + | + | - | + | + | + (баг) |

### Запросы

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `get(index)` | + | + (op) | + (op) | + (op) | + (op) | + (op) |
| `get(from, to)` → BitSet | + | - | - | - | - | - |
| `isEmpty` | + (метод) | + (свойство) | + (свойство) | + (свойство) | + | + (`empty`) |
| `size` / capacity | + `size()` | + `size` (свойство) | + `size` | + `size` (word count, не capacity в битах) | - | + `size()` |
| `length()` | + | - | - | - | + | + (`length` свойство) |
| `cardinality()` | + | **-** | - | + | + | - |
| `lastTrueIndex` | - (= `length()-1`) | + | - | - | + (`last()`) | - |

### Навигация по битам

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `nextSetBit(from)` | + | + | + | - | ~ (`first(from, to, true)`) | + |
| `nextClearBit(from)` | + | + | + | - | ~ (`first(from, to, false)`) | + |
| `previousSetBit(from)` | + | + | - | - | - | - |
| `previousClearBit(from)` | + | + | - | - | - | - |

### Bulk-операции (побитовые)

Легенда дополнение: **(ip)** = in-place, **(new)** = возвращает новый объект. Для BitVector (lib): `and`/`or`/`xor` на базовом типе всегда возвращают новый объект; in-place варианты (`mutateAnd`/`mutateOr`/`mutateXor`/`mutateAndNot`) доступны только на `MutableBitVector`.

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `and(other)` | + (ip) | + (ip) | + (ip) | + (ip) | + (new) / `mutateAnd` (ip) | - |
| `or(other)` | + (ip) | + (ip) | + (ip) | + (ip) | + (new) / `mutateOr` (ip) | - |
| `xor(other)` | + (ip) | + (ip) | + (ip) | - | + (new) / `mutateXor` (ip) | - |
| `andNot(other)` | + (ip) | + (ip) | + (ip) | + (ip) | `mutateAndNot` (ip only) | - |
| `intersects(other)` | + | + | + | + | + | - |
| `contains(other)` (subset) | - | - | - | + | + | - |
| `orWithFilter + changed` | - | - | - | + | - | - |

### Итерация

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `stream()` | + (Java 8) | - | - | - | - | - |
| `Iterable` / `iterator()` | - | - | - | - | + (`IntIterator`) | - |
| `forEachBit { }` | - (ext) | - | - | + (inline) | + (inline) | - |
| `forEachZeroBit { }` | - | - | - | - | + (inline) | - |
| Callback-итерация | - | - | - | - | + (`forEachBitBreakable`) | + (`iterateSetBits`/`iterateClearBits`) |
| `mapEachBit { }` | - (ext) | - | - | - | - | - |
| `forEachWord { }` | - | - | - | + | - | - |

### Конвертация и копирование

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `toByteArray()` | + | - | - | - | - | + |
| `toLongArray()` | + | - | - | - | - | - |
| `toIntArray()` / `words` | - | - | - | - | + (`words` свойство) | - |
| `clone()` / `copy()` | + (`clone()`) | - | - | + (`copy()`) | + (`copy()`) | - |

### Объектные методы

| Операция | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `equals()` | + | + | - | + (size-aware) | + | - |
| `hashCode()` | + | + | - | + | + | - |
| `toString()` | + `{0, 2, 5}` | + `[0\|2\|5]` | - | - | + | + `", 0, 2, 5"` (без скобок) |

### Интерфейсы

| Интерфейс | java.util.BitSet | kotlin.native.BitSet | Wasm BitSet | CustomBitSet | BitVector (lib) | KmpIO |
|---|---|---|---|---|---|---|
| `Cloneable` | + | - | - | - | - | - |
| `Serializable` | + | - | - | - | - | - |
| `Collection<Int>` | - | - | - | - | - | - |
| `Set<Int>` | - | - | - | - | - | - |
| `Iterable<Int>` | - | - | - | - | + | - |

---

## 7. Ключевые выводы

### Общие решения (присутствуют в большинстве реализаций)

1. **Мутабельность** — большинство реализаций только мутабельные. BitVector — единственная библиотека с отдельным read-only API surface: sealed base type `BitVector` и mutable `MutableBitVector`. При этом immutability обеспечивается только на уровне типовой системы: единственная concrete-реализация — `MutableBitVector`. Кроме того, `words: IntArray` имеет public getter, что позволяет мутировать содержимое массива извне даже через `BitVector`-ссылку.
2. **Динамический размер** — большинство растут автоматически. KmpIO — исключение со смешанной семантикой: внутренний массив расширяется, но конструкторный `numberOfBits` используется как логическая граница в `toByteArray()` и `iterateClearBits()`.
3. **LongArray как внутреннее представление** — все, кроме BitVector (IntArray; предположительно для лучшей JS-производительности — гипотеза).
4. **Интерфейсы коллекций** — BitVector реализует `Iterable<Int>`; остальные реализации не реализуют `Set<Int>`, `Collection<Int>` или `Iterable<Int>`.
5. **Core bulk-операции** — `and`, `or`, `andNot` присутствуют почти везде (кроме KmpIO); `xor` также отсутствует у CustomBitSet. Все in-place; BitVector — смешанный случай: на базовом типе `and`/`or`/`xor` всегда возвращают новый объект, а `MutableBitVector` добавляет отдельные in-place методы `mutateAnd`/`mutateOr`/`mutateXor`/`mutateAndNot`.
6. **Operator `[]` для get** — Kotlin-реализации (native, custom, bitvector, kmpio) используют `operator fun get`.

### Уникальные решения

| Решение | Где | Значимость для нового API |
|---|---|---|
| Initializer-конструктор `BitSet(n) { ... }` | kotlin.native.BitSet | Высокая — удобный Kotlin-паттерн |
| `IntRange` перегрузки | kotlin.native.BitSet | Высокая — ключевая Kotlin-идиома |
| **Read-only/Mutable разделение** | BitVector | Высокая — единственная библиотека с разделением на read-only base type и mutable subtype (immutability на уровне типов; ослаблена public getter'ом `words`) |
| **`Iterable<Int>`** | BitVector | Высокая — интеграция с Kotlin collections |
| `forEachBit { }` inline | CustomBitSet, BitVector, BitSetUtil(ext) | Высокая — независимо реализован в 3 из 6 библиотек; подтверждение usage-данными — в шаге 4 |
| Callback-итерация с прерыванием | BitVector (`forEachBitBreakable`), KmpIO (`iterateSetBits`) | Средняя — расширенный паттерн итерации |
| `contains(another)` (subset check) | CustomBitSet, BitVector | Средняя — полезно для dataflow analysis |
| `orWithFilterHasChanged()` | CustomBitSet | Низкая — специализированная операция |
| `operator fun set(index, value)` | CustomBitSet, BitVector, KmpIO | Высокая — `bitSet[i] = true` синтаксис |
| `IntArray` вместо `LongArray` | BitVector | Важно для JS-стратегии |
| `valueOf(ByteArray)` / `ByteBuffer` | KmpIO | Средняя — для binary protocols |

### Пробелы kotlin.native.BitSet

Операции, отсутствующие в kotlin.native.BitSet, но присутствующие в java.util.BitSet и/или внутренних реализациях:

| Пробел | Приоритет | Обоснование |
|---|---|---|
| `cardinality()` (кол-во set-битов) | Высокий | Есть в Java и CustomBitSet; базовая операция |
| Итерация (`forEachBit`, `stream`, `Iterable`) | Высокий | Добавлен как extension в BitSetUtil, встроен в CustomBitSet и BitVector |
| `copy()` / `clone()` | Высокий | Есть в CustomBitSet и BitVector; базовая потребность |
| `valueOf()` фабрики | Средний | Есть в Java и CustomBitSet; нужно для interop |
| `toByteArray()` / `toLongArray()` | Средний | Есть в Java; нужно для serialization/interop |
| `get(from, to)` → BitSet | Низкий | Предположительно редко используется (гипотеза, требует проверки usage-данными в шаге 4) |
| `operator fun set(index, value)` | Высокий | `bitSet[i] = true` — ожидаемый Kotlin-синтаксис |

### Открытые вопросы для последующих шагов

1. **Баг в `kotlin.native.BitSet.flip(range)`** (строка 220) — первый элемент и последний перепутаны. Не критично для дизайна нового API, но нужно учесть при миграции.
2. **Семантика `size`** — в Java это capacity (кратна 64), в Native — кол-во доступных битов. Кроме того, в Native `clear(index)` / `clear(from, to)` / `clear(range)` за пределами `size` расширяют его (делегируют в `set(..., false)` → `ensureCapacity()`); parameterless `clear()` только обнуляет массив без изменения `size`. В Java `clear(index)` за пределами `wordsInUse` — no-op. Разница не только терминологическая, но и поведенческая. Новый API должен чётко определить, что экспонировать (→ шаг 8).
3. **`toString()` формат** — Java `{0, 2, 5}` vs Native `[0|2|5]`. Выбор формата → шаг 8.
4. **IntArray vs LongArray** на JS — BitVector использует IntArray, предположительно для лучшей производительности на JS из-за Long-эмуляции (гипотеза, требует проверки бенчмарками → шаг 10).
