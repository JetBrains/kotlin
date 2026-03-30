# C# `System.Collections.BitArray` -- детальный анализ API surface

## Резюме

`BitArray` -- мутабельная коллекция фиксированного размера с явным resize через settable `Length`. Класс `sealed`, реализует `ICollection`, `IEnumerable`, `ICloneable`. Внутреннее представление -- `int[]` (32-битные слова). Итерация через `GetEnumerator()` возвращает **bool-значения** (не индексы set-битов), что принципиально отличается от Java `BitSet.stream()`. Bulk-операции (`And`, `Or`, `Xor`, `Not`) работают in-place и возвращают `this` для chaining, но **требуют одинаковых длин** (`ArgumentException` при несовпадении) -- существенное ограничение по сравнению с Java. В .NET Core 2.0+ добавлены `LeftShift`/`RightShift`, в .NET 8 -- `HasAllSet`/`HasAnySet`, в .NET 11 (preview) -- `PopCount`. Нет `AndNot`, `Intersects`, навигации по битам (`nextSetBit`/`previousSetBit`), factory methods (`valueOf`), `stream()` индексов.

---

## 1. Объявление класса

```csharp
public sealed class BitArray : ICloneable, System.Collections.ICollection
```

- **`sealed`** -- нельзя наследовать.
- **Reference type** (class) -- размещается в куче.
- **Пространство имён:** `System.Collections`.
- **Сборка:** `System.Collections.dll` (в .NET Core+), `mscorlib.dll` (в .NET Framework).

### Реализуемые интерфейсы

| Интерфейс | Что предоставляет | Примечания |
|---|---|---|
| `ICollection` | `Count`, `IsSynchronized`, `SyncRoot`, `CopyTo(Array, int)` | Non-generic; `Count` -- read-only через интерфейс |
| `IEnumerable` | `GetEnumerator()` | Non-generic; включает поддержку `foreach` и LINQ (через `Cast<bool>()`) |
| `ICloneable` | `Clone()` | Shallow copy (но внутренний `int[]` копируется, т.е. фактически deep copy для данных) |

**Не реализует:** `ICollection<T>`, `IEnumerable<T>`, `IList`, `ISet`, `IReadOnlyCollection<T>`.

> **Замечание:** `IEnumerable` (non-generic) требует boxing при итерации через `foreach` без `Cast<bool>()`. Для использования LINQ необходим вызов `.Cast<bool>()`, что добавляет аллокации.

---

## 2. Конструкторы

| # | Сигнатура | Описание |
|---|---|---|
| 1 | `BitArray(int length)` | Создаёт BitArray на `length` бит, все инициализированы `false` |
| 2 | `BitArray(int length, bool defaultValue)` | Создаёт BitArray на `length` бит, все инициализированы в `defaultValue` |
| 3 | `BitArray(byte[])` | Копирует биты из массива byte; `Length` = `bytes.Length * 8` |
| 4 | `BitArray(bool[])` | Один bool -> один бит; `Length` = `bools.Length` |
| 5 | `BitArray(int[])` | Копирует биты из массива int (32-битных слов); `Length` = `ints.Length * 32` |
| 6 | `BitArray(BitArray)` | Copy constructor; копирует все биты из другого BitArray |

### Сравнение с Java

| Аспект | Java `BitSet` | C# `BitArray` |
|---|---|---|
| Пустой конструктор | `BitSet()` -- ёмкость 64, длина 0 | Нет -- **обязательно указывать длину** |
| Ёмкость vs длина | Аргумент `nbits` -- ёмкость (hint) | Аргумент `length` -- точный размер |
| Из `long[]` | `BitSet.valueOf(long[])` | Нет прямого аналога (`int[]` -- 32 бита) |
| Из `LongBuffer` | `BitSet.valueOf(LongBuffer)` | Нет |
| Из `ByteBuffer` | `BitSet.valueOf(ByteBuffer)` | Нет |
| Copy | `clone()` | `BitArray(BitArray)` + `Clone()` |
| Default value | Всегда `false` | Можно задать `true` через `BitArray(n, true)` |

> **Ключевое отличие:** Java `BitSet` не требует указания размера и автоматически растёт. C# `BitArray` требует явного размера при создании.

---

## 3. Свойства (Properties)

| Свойство | Тип | Get/Set | Описание |
|---|---|---|---|
| `Count` | `int` | get | Количество элементов (битов). Read-only. |
| `Length` | `int` | get/set | Количество элементов (битов). **Settable** -- основной механизм resize. |
| `IsReadOnly` | `bool` | get | Всегда `false` |
| `IsSynchronized` | `bool` | get | Всегда `false` |
| `SyncRoot` | `object` | get | Объект для ручной синхронизации |
| `Item[int]` (indexer) | `bool` | get/set | Синтаксический сахар `bits[i]`; эквивалентен `Get(i)` / `Set(i, value)` |

### Count vs Length -- ключевое различие

`Length` и `Count` возвращают **одно и то же значение**. Разница -- только в mutability:

| Свойство | Settable? | Источник |
|---|---|---|
| `Count` | Нет (read-only, через `ICollection`) | `ICollection.Count` |
| `Length` | **Да** (get/set) | Собственное свойство `BitArray` |

**Поведение при установке `Length`:**

| Сценарий | Поведение |
|---|---|
| `Length = N` где `N > текущего` | Расширение; новые биты инициализируются `false` |
| `Length = N` где `N < текущего` | **Усечение**; биты с индексами >= N удаляются безвозвратно |
| `Length = N` где `N < 0` | `ArgumentOutOfRangeException` |

> **Сравнение с Java:** `java.util.BitSet` не имеет аналога settable `Length`. Размер растёт автоматически при `set()` и не уменьшается. В Java есть три отдельных понятия: `size()` (ёмкость), `length()` (logical size = max set bit + 1), `cardinality()` (popcount). В C# `Length`/`Count` -- это **фиксированный размер массива**, не зависящий от содержимого.

### Семантика размера: Java vs C#

| Понятие | Java `BitSet` | C# `BitArray` |
|---|---|---|
| Ёмкость (внутренняя) | `size()` (кратна 64) | Скрыта (кратна 32, т.к. `int[]`) |
| Логический размер | `length()` (max set bit + 1) | **Нет аналога** |
| Фиксированный размер | Нет | `Length` / `Count` |
| Количество set-битов | `cardinality()` | **`HasAnySet()`/`HasAllSet()`** (с .NET 8); **`PopCount()`** (с .NET 11) |

---

## 4. Методы -- полный каталог

### 4.1. Чтение/запись отдельных битов

| Сигнатура | Возвращает | Описание |
|---|---|---|
| `bool Get(int index)` | `bool` | Значение бита по индексу |
| `void Set(int index, bool value)` | `void` | Установить бит в указанное значение |
| `void SetAll(bool value)` | `void` | Установить все биты в `value` |

**Indexer `this[int index]`** -- эквивалентен `Get(index)` / `Set(index, value)`:
```csharp
bool val = bits[5];     // == bits.Get(5)
bits[5] = true;         // == bits.Set(5, true)
```

> **Отличие от Java:** Java `BitSet.set(int)` устанавливает в `true` (без параметра), а `set(int, bool)` -- в указанное значение. Также Java имеет range-варианты `set(from, to)` и `set(from, to, value)`. C# не имеет range-вариантов `Set()` -- только `SetAll()` для всех битов.

### 4.2. Bulk-операции (побитовые, in-place)

| Сигнатура | Возвращает | Описание | Модифицирует `this` |
|---|---|---|---|
| `BitArray And(BitArray value)` | `BitArray` (= `this`) | `this &= value` | Да |
| `BitArray Or(BitArray value)` | `BitArray` (= `this`) | `this \|= value` | Да |
| `BitArray Xor(BitArray value)` | `BitArray` (= `this`) | `this ^= value` | Да |
| `BitArray Not()` | `BitArray` (= `this`) | `this = ~this` | Да |

**Критически важные детали:**

1. **Возвращают `this`** -- позволяет chaining: `bits.And(a).Or(b).Not()`.
2. **In-place** -- модифицируют текущий объект, `value` не изменяется.
3. **Требуют одинаковой длины** -- `And`, `Or`, `Xor` бросают `ArgumentException` если `value.Length != this.Length`. Это принципиальное отличие от Java, где размеры могут различаться.
4. **`Not()` не принимает аргументов** -- инвертирует все биты текущего BitArray.

| Аспект | Java `BitSet` | C# `BitArray` |
|---|---|---|
| Return type | `void` (кроме `and`, `or`, `xor` в Java не возвращают) | `BitArray` (= `this`, chaining) |
| Разные размеры | Допустимы | `ArgumentException` |
| `andNot()` | Есть | **Нет** |
| `intersects()` | Есть | **Нет** |

> **Замечание:** Java `BitSet` bulk-методы возвращают `void`, а C# `BitArray` -- возвращают `this`. Отсутствие `AndNot` и `Intersects` в C# заметно при реализации множественных операций над BitArray.

### 4.3. Shift-операции (.NET Core 2.0+)

| Сигнатура | Возвращает | Описание | Модифицирует `this` |
|---|---|---|---|
| `BitArray LeftShift(int count)` | `BitArray` (= `this`) | Сдвиг всех битов влево на `count` позиций | Да |
| `BitArray RightShift(int count)` | `BitArray` (= `this`) | Сдвиг всех битов вправо на `count` позиций | Да |

- Вакантные позиции заполняются `false`.
- Биты, сдвинутые за границу `Length`, теряются.
- `count < 0` -- `ArgumentOutOfRangeException`.
- Размер BitArray **не меняется** после сдвига.
- Возвращают `this` для chaining.
- **Доступно с:** .NET Core 2.0 / .NET Standard 2.1.

> **Сравнение:** Java `BitSet` не имеет shift-операций. В C# это полностью in-place операции без изменения длины.

### 4.4. Запросы состояния (.NET 8+)

| Сигнатура | Возвращает | Описание | Добавлено |
|---|---|---|---|
| `bool HasAllSet()` | `bool` | `true`, если все биты == `true` (или BitArray пуст) | .NET 8 |
| `bool HasAnySet()` | `bool` | `true`, если хотя бы один бит == `true` | .NET 8 |
| `int PopCount()` | `int` | Количество установленных битов (cardinality) | .NET 11 (preview) |

> **Замечание:** `HasAllSet()` возвращает `true` для пустого BitArray (Length == 0) -- аналогично "for-all" квантору над пустым множеством. `PopCount()` -- прямой аналог Java `BitSet.cardinality()`, но добавлен только в .NET 11.

### 4.5. Копирование и клонирование

| Сигнатура | Возвращает | Описание |
|---|---|---|
| `object Clone()` | `object` | Shallow copy. Фактически deep copy данных, т.к. внутренний `int[]` копируется |
| `void CopyTo(Array array, int index)` | `void` | Копирует данные в целевой массив, начиная с `index` |

**`CopyTo` поддерживает три типа целевых массивов:**

| Тип массива | Что копируется |
|---|---|
| `bool[]` | Каждый бит -> один `bool` элемент |
| `int[]` | Биты упакованы по 32 в каждый `int` |
| `byte[]` | Биты упакованы по 8 в каждый `byte` |
| Другие типы | `ArgumentException: "Only supported array types for CopyTo on BitArrays are Boolean[], Int32[] and Byte[]."` |

> **Сравнение с Java:** Java `BitSet` имеет `toByteArray()` и `toLongArray()` -- возвращают новый массив. C# `BitArray.CopyTo()` копирует в предоставленный массив, не создавая нового. Нет прямого `ToByteArray()` / `ToIntArray()` -- нужно создать массив и вызвать `CopyTo`.

### 4.6. Итерация

| Сигнатура | Возвращает | Описание |
|---|---|---|
| `IEnumerator GetEnumerator()` | `IEnumerator` | Non-generic enumerator; `Current` содержит **boxed `bool`** |

**Ключевая характеристика:** `GetEnumerator()` итерирует **все биты по порядку**, возвращая **`bool` значения** (`true`/`false`). Это _не_ итерация по индексам установленных битов.

```csharp
// Итерация возвращает bool-значения для КАЖДОГО бита
foreach (bool bit in myBitArray)  // Требует Cast<bool>() для strict typing
{
    Console.Write(bit ? "1" : "0");
}
// Вывод для BitArray {true, false, true}: "101"
```

| Аспект | Java `BitSet` | C# `BitArray` |
|---|---|---|
| `stream()` | `IntStream` -- индексы set-битов | Нет |
| `GetEnumerator()` | Нет | `IEnumerator` -- **bool-значения всех битов** |
| `nextSetBit()` | Есть | **Нет** |
| `previousSetBit()` | Есть | **Нет** |
| `nextClearBit()` | Есть | **Нет** |
| `previousClearBit()` | Есть | **Нет** |

> **Критический пробел:** C# `BitArray` не имеет средств навигации по set/clear битам. Для нахождения установленных битов необходимо итерировать все биты и проверять каждый, что неэффективно для sparse BitArrays.

### 4.7. Объектные методы (унаследованные от Object)

| Сигнатура | Описание | Переопределён? |
|---|---|---|
| `bool Equals(object)` | Сравнение ссылок | **Нет** (reference equality!) |
| `int GetHashCode()` | Хэш объекта | **Нет** (identity hash!) |
| `string ToString()` | Строковое представление | **Нет** (возвращает `"System.Collections.BitArray"`) |

> **Принципиальное отличие от Java:** `BitArray` **не переопределяет** `Equals`, `GetHashCode`, `ToString`. Java `BitSet` реализует value-based equality, content-based hashCode и `toString()` в формате `{0, 2, 5}`. Это делает C# `BitArray` непригодным для использования в качестве ключа в словарях/хэш-множествах.

### 4.8. Explicit interface implementations

| Сигнатура | Описание |
|---|---|
| `ICollection.CopyTo(Array, int)` | Явная реализация `ICollection.CopyTo` |
| `ICollection.Count` | Явная реализация `ICollection.Count` |
| `ICollection.IsSynchronized` | Явная реализация `ICollection.IsSynchronized` |
| `ICollection.SyncRoot` | Явная реализация `ICollection.SyncRoot` |

### 4.9. Extension methods (через IEnumerable)

Благодаря `IEnumerable` доступны LINQ extension methods:

| Метод | Описание |
|---|---|
| `Cast<TResult>()` | Приведение элементов к `bool` (`bits.Cast<bool>()`) |
| `OfType<TResult>()` | Фильтрация по типу |
| `AsParallel()` | Параллельная итерация |
| `AsQueryable()` | Конвертация в `IQueryable` |

> **Замечание:** Т.к. `BitArray` реализует non-generic `IEnumerable`, LINQ-методы вроде `.Where()`, `.Select()`, `.Count()` требуют предварительного вызова `.Cast<bool>()`, что добавляет boxing/unboxing overhead.

---

## 5. Thread Safety

- Статические (`static`) члены -- thread-safe.
- Экземплярные члены -- **не thread-safe**.
- Synchronized wrapper **не предоставляется** (в отличие от `ArrayList.Synchronized()`).
- Итерация intrinsically не thread-safe; модификация во время итерации -> `InvalidOperationException`.
- Для обеспечения thread safety документация рекомендует внешнюю блокировку через `SyncRoot`.

---

## 6. Полная сравнительная таблица: C# BitArray vs Java BitSet

| Категория | Java `BitSet` | C# `BitArray` |
|---|---|---|
| **Тип** | `class` (non-sealed) | `sealed class` |
| **Мутабельность** | Мутабельный | Мутабельный |
| **Immutable вариант** | Нет | Нет |
| **Размер** | Динамический (auto-grow) | Фиксированный (explicit resize через `Length`) |
| **Внутреннее представление** | `long[]` (64 бита) | `int[]` (32 бита) |
| **Интерфейсы** | `Cloneable, Serializable` | `ICollection, IEnumerable, ICloneable` |
| **Итерация: set-битов** | `stream()` -> `IntStream` | **Нет** |
| **Итерация: всех значений** | Нет | `GetEnumerator()` -> `bool` |
| **Навигация** | `nextSetBit`, `nextClearBit`, `previousSetBit`, `previousClearBit` | **Нет** |
| **Cardinality** | `cardinality()` | `PopCount()` (с .NET 11) |
| **isEmpty** | `isEmpty()` | Через `HasAnySet()` (с .NET 8) или итерацию |
| **Indexer** | Нет (`get(i)`, `set(i, v)`) | `this[int]` (`bits[i]`) |
| **Range operations** | `set(from, to)`, `clear(from, to)`, `flip(from, to)`, `get(from, to)` | **Нет** |
| **Shift** | Нет | `LeftShift(n)`, `RightShift(n)` (с .NET Core 2.0) |
| **AndNot** | `andNot(BitSet)` | **Нет** |
| **Intersects** | `intersects(BitSet)` | **Нет** |
| **Size mismatch** | Допустим | `ArgumentException` |
| **Bulk return type** | `void` | `BitArray` (= `this`, chaining) |
| **Factory methods** | `valueOf(long[])`, `valueOf(byte[])`, `valueOf(LongBuffer)`, `valueOf(ByteBuffer)` | Нет (только конструкторы) |
| **Конвертация** | `toByteArray()`, `toLongArray()` | `CopyTo(bool[]/int[]/byte[], offset)` |
| **Equals/HashCode** | Value-based (content) | **Reference-based (identity)** |
| **ToString** | `{0, 2, 5}` (индексы set-битов) | `"System.Collections.BitArray"` (тип) |
| **Serializable** | Да (`Serializable`) | Нет (не `[Serializable]` в .NET Core+) |
| **Thread safety** | Не thread-safe | Не thread-safe |

---

## 7. Анализ по осям сравнения

### 7.1. Мутабельность

**Только мутабельный.** Нет immutable-варианта, нет `AsReadOnly()`, `IsReadOnly` всегда `false`. Все bulk-операции и `Set`/`SetAll` модифицируют объект in-place. Клонирование через `Clone()` или copy constructor -- единственный способ получить "snapshot".

### 7.2. Модель размера

**Фиксированный с явным resize:**
- Размер определяется при создании.
- Автоматический рост **отсутствует** -- обращение за пределы `Length` -> `ArgumentOutOfRangeException`.
- Изменение размера -- через `Length` setter (единственный механизм).
- При увеличении: новые биты = `false`.
- При уменьшении: биты усекаются.
- `Count` и `Length` всегда равны.

Это hybrid-модель: не полностью фиксированная (как `std::bitset<N>`), не полностью динамическая (как `java.util.BitSet`). Resize явный, но возможный.

### 7.3. Интерфейсы коллекций

`ICollection` + `IEnumerable` дают:
- Участие в LINQ-pipeline (через `.Cast<bool>()`).
- `foreach` поддержка.
- `CopyTo` для экспорта.
- `Count` для размера.

Однако:
- Non-generic интерфейсы -> boxing при итерации.
- Нет `ICollection<bool>` -> нет `Add`, `Remove`, `Contains`.
- Нет `IEnumerable<bool>` -> LINQ только через `Cast<>()`.
- Нет `IList<bool>` -> нет `IndexOf`, `Insert`.
- Нет `ISet<int>` -> не может представлять множество целых чисел.

### 7.4. Итерация

`GetEnumerator()` возвращает `IEnumerator` (non-generic). `Current` -- boxed `object`, содержащий `bool`.

**Итерация идёт по всем битам**, включая `false`. Элемент -- **bool-значение**, не индекс.

```csharp
// C#: итерирует bool-значения
foreach (bool b in bitArray.Cast<bool>()) { ... }

// Java: итерирует индексы set-битов
bitSet.stream().forEach(index -> ...);
```

**Нет возможности эффективно итерировать только set-биты** -- нет `nextSetBit()`, нет `stream()` индексов. Для sparse BitArray (мало set-битов, большая длина) это значительная проблема производительности.

### 7.5. Операторы и синтаксический сахар

| Средство | Поддержка |
|---|---|
| Indexer `[]` | Да: `bits[i]` для чтения и записи |
| Operator overloading (`&`, `\|`, `^`, `~`) | **Нет** |
| Collection initializer | **Нет** (нет `Add`) |
| Pattern matching | **Нет** |
| Chaining | Да (bulk-методы возвращают `this`) |

> **Замечание о chaining:** Возврат `this` из `And`/`Or`/`Xor`/`Not`/`LeftShift`/`RightShift` -- уникальная для C# особенность (Java `BitSet` bulk-методы возвращают `void`). Позволяет: `bits.And(mask).Not().LeftShift(2)`.

### 7.6. Сериализация и interop

**Входные конвертации (конструкторы):**

| Источник | Конструктор |
|---|---|
| `byte[]` | `BitArray(byte[])` |
| `bool[]` | `BitArray(bool[])` |
| `int[]` | `BitArray(int[])` |
| `BitArray` | `BitArray(BitArray)` |
| `long[]` | **Нет** |

**Выходные конвертации (`CopyTo`):**

| Цель | Метод |
|---|---|
| `bool[]` | `CopyTo(boolArray, offset)` |
| `int[]` | `CopyTo(intArray, offset)` |
| `byte[]` | `CopyTo(byteArray, offset)` |
| `long[]` | **Нет** |

> **Замечание:** отсутствие `long[]` interop связано с использованием `int[]` в качестве внутреннего представления (32-битные слова vs 64-битные в Java).

---

## 8. Хронология эволюции API

| Версия .NET | Добавлено | Комментарий |
|---|---|---|
| .NET Framework 1.0 | Core API: Get, Set, SetAll, And, Or, Xor, Not, Clone, CopyTo, GetEnumerator, Length, Count, Item[] | Первоначальный дизайн |
| .NET Core 2.0 / .NET Standard 2.1 | `LeftShift(int)`, `RightShift(int)` | Shift-операции, in-place, возвращают `this` |
| .NET 8 | `HasAllSet()`, `HasAnySet()` | Быстрые проверки наличия/отсутствия set-битов |
| .NET 11 (preview) | `PopCount()` | Аналог `cardinality()` из Java |

> **Тенденция:** API постепенно расширяется, но очень медленно. За 25 лет добавлено лишь 5 новых методов. Базовые пробелы (range operations, navigation, value equality) не адресованы.

---

## 9. `System.Collections.Specialized.BitVector32`

Отдельная структура для оптимизированной работы с 32 битами.

### Объявление

```csharp
public struct BitVector32 : IEquatable<BitVector32>
```

### Ключевые отличия от BitArray

| Аспект | `BitArray` | `BitVector32` |
|---|---|---|
| Тип | `sealed class` (reference) | `struct` (value) |
| Размещение | Куча (heap) | Стек (stack) |
| Размер | Произвольный | **Ровно 32 бита** |
| Интерфейсы | `ICollection, IEnumerable, ICloneable` | `IEquatable<BitVector32>` |
| Использование | Общего назначения | Bit flags и packed integers |
| Производительность | Аллокация + GC | Без аллокаций |
| Итерация | `GetEnumerator()` | Нет |

### Конструкторы

| Сигнатура | Описание |
|---|---|
| `BitVector32(int data)` | Инициализация из целого числа |
| `BitVector32(BitVector32 value)` | Copy constructor |

### Свойства

| Свойство | Тип | Описание |
|---|---|---|
| `Data` | `int` | Все 32 бита как целое число |
| `Item[int mask]` | `bool` (get/set) | Чтение/запись бита по маске |
| `Item[BitVector32.Section]` | `int` (get/set) | Чтение/запись секции (packed integer) |

### Режимы работы

`BitVector32` может использоваться **в одном из двух режимов** (не одновременно):

**Режим 1 -- Bit flags (маски):**
```csharp
int flag1 = BitVector32.CreateMask();      // 1
int flag2 = BitVector32.CreateMask(flag1); // 2
int flag3 = BitVector32.CreateMask(flag2); // 4
bv[flag1] = true;  // Установить бит 0
bv[flag3] = true;  // Установить бит 2
```

**Режим 2 -- Sections (packed integers):**
```csharp
BitVector32.Section s1 = BitVector32.CreateSection(6);      // 3 бита, макс 6
BitVector32.Section s2 = BitVector32.CreateSection(3, s1);  // 2 бита, макс 3
bv[s1] = 5;  // Записать 5 в первую секцию
bv[s2] = 3;  // Записать 3 во вторую секцию
```

### Статические методы

| Сигнатура | Описание |
|---|---|
| `static int CreateMask()` | Создать маску для первого бита (= 1) |
| `static int CreateMask(int previous)` | Создать маску для следующего бита (= previous << 1) |
| `static Section CreateSection(short maxValue)` | Создать первую секцию с указанным макс. значением |
| `static Section CreateSection(short maxValue, Section previous)` | Создать секцию после предыдущей |
| `static string ToString(BitVector32 value)` | Строковое представление |

### Nested struct `BitVector32.Section`

```csharp
public readonly struct Section : IEquatable<Section>
```

Описывает "окно" в `BitVector32` -- offset и mask для packed integer.

| Свойство | Тип | Описание |
|---|---|---|
| `Mask` | `short` | Битовая маска секции |
| `Offset` | `short` | Смещение от начала |

### Вывод по BitVector32

`BitVector32` -- специализированная структура для performance-critical кода, работающего ровно с 32 битами. Не является альтернативой `BitArray` для общих задач, скорее дополняет его для сценариев, где известно, что битов <= 32, и нужна zero-allocation семантика. Двухрежимная работа (bit flags / packed integers) -- уникальная особенность, не встречающаяся в BitSet-API других языков.

---

## 10. Ключевые выводы для дизайна Kotlin BitSet

### Сильные стороны C# BitArray (стоит рассмотреть)

1. **Indexer `this[int]`** -- удобный синтаксический сахар. В Kotlin аналог -- `operator fun get(index)` / `operator fun set(index, value)`.
2. **Chaining через return `this`** -- `And`/`Or`/`Xor`/`Not` возвращают `this`. Позволяет fluent-стиль. В Kotlin можно реализовать через `apply` или явный return.
3. **Конструктор `BitArray(int, bool)`** -- default value при создании. Java `BitSet` не имеет аналога (всегда `false`).
4. **Shift-операции** -- `LeftShift`/`RightShift` -- полезны, отсутствуют в Java `BitSet`.
5. **Множественные входные форматы** -- конструкторы из `byte[]`, `bool[]`, `int[]`.
6. **`CopyTo` с множественными целевыми типами** -- `bool[]`, `int[]`, `byte[]`.

### Слабые стороны C# BitArray (избегать)

1. **Нет value-based equality** -- `Equals`/`GetHashCode` не переопределены. Для Kotlin stdlib это было бы серьёзной ошибкой.
2. **Нет `ToString` с содержимым** -- возвращает имя типа. Бесполезно для отладки.
3. **Нет навигации по битам** -- отсутствие `nextSetBit`/`previousSetBit` -- один из крупнейших пробелов.
4. **Нет range operations** -- нет `set(from, to)`, `clear(from, to)`, `flip(from, to)`.
5. **Нет `AndNot`** -- частая операция (set difference).
6. **Нет `Intersects`** -- нужно вручную проверять через `And` + `HasAnySet`.
7. **Нет iteration by set-bit indices** -- `GetEnumerator` возвращает `bool`-значения, не индексы. Для sparse BitArray -- неприемлемая производительность.
8. **Strict size matching для bulk ops** -- `ArgumentException` при разных длинах. Java допускает разные размеры и это удобнее.
9. **Non-generic `IEnumerable`** -- boxing overhead при итерации.
10. **`int[]` вместо `long[]`** -- на 64-битных платформах менее эффективно.
11. **Нет serialization support** в .NET Core+.
12. **Чрезвычайно медленная эволюция API** -- за 25 лет добавлено 5 методов. `PopCount()` (аналог `cardinality()`) появился только в .NET 11.

### Открытые вопросы

1. **Settable Length** -- стоит ли Kotlin BitSet поддерживать resize? Java -- auto-grow, C# -- explicit resize. Kotlin может выбрать третий путь (immutable size + builder/factory для resize).
2. **Итерация: `bool`-значения vs индексы set-битов** -- C# выбрал первое, Java -- второе. Для Kotlin стоит рассмотреть оба: `Iterable<Int>` (индексы) + `forEachBit {}` (как в BitSetUtil.kt).
3. **Return type bulk операций** -- `void` (Java) vs `this` (C#). Kotlin может вернуть `Unit` для мутабельного варианта и новый объект для immutable.
