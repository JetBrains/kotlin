# C# `System.Collections.BitArray` -- детальный анализ API surface

## Резюме

`BitArray` -- мутабельная коллекция фиксированного размера с явным resize через settable `Length`. Класс `sealed`, реализует `ICollection`, `IEnumerable`, `ICloneable`. Внутреннее представление -- `byte[]` с .NET 10 (до .NET 10 — `int[]`; выравнен по `sizeof(int)`; bulk-операции (And/Or/Xor/Not) векторизованы через `Vector512`/`Vector256`/`Vector128` поверх `byte[]`, remainder path — через `MemoryMarshal.Cast<byte, int>`). Итерация через `GetEnumerator()` возвращает **bool-значения** (не индексы set-битов), что принципиально отличается от Java `BitSet.stream()`. Bulk-операции (`And`, `Or`, `Xor`, `Not`) работают in-place и возвращают `this` для chaining; бинарные операции (`And`, `Or`, `Xor`) **требуют одинаковых длин** (`ArgumentException` при несовпадении) -- существенное ограничение по сравнению с Java; `Not()` — unary, аргументов не принимает и просто инвертирует текущий bit array. В .NET Core 2.0+ добавлены `LeftShift`/`RightShift`, в .NET 8 -- `HasAllSet`/`HasAnySet`, в .NET 10 -- `PopCount`; на уровне runtime в .NET 10 также добавлен `CollectionsMarshal.AsBytes(BitArray)` (zero-copy `Span<byte>` над байтами, необходимыми для текущего `Length`; неиспользованная capacity не экспонируется). Нет `AndNot`, `Intersects`, навигации по битам (`nextSetBit`/`previousSetBit`), factory methods (`valueOf`), `stream()` индексов.

**Входные данные:** [`step-01-kotlin-implementations.md`](step-01-kotlin-implementations.md) (Java `BitSet` как baseline для сравнения).

**См. также:** [`step-02-cross-language.md`](step-02-cross-language.md) (зонтичный кросс-языковой обзор, в который входит данный артефакт).

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
| `ICollection` | `Count`, `IsSynchronized`, `SyncRoot`, `CopyTo(Array, int)` | Non-generic; `BitArray` предоставляет эти четыре члена как обычные public properties/methods. Microsoft Learn дополнительно показывает интерфейсные страницы/карточки для этих же членов — это документационная проекция `ICollection`, а не отдельные explicit interface implementations |
| `IEnumerable` | `GetEnumerator()` | Non-generic; включает поддержку `foreach` и LINQ (через `Cast<bool>()`) |
| `ICloneable` | `Clone()` | Shallow copy (но внутренний `byte[]` копируется, т.е. фактически deep copy для данных) |

**Не реализует:** `ICollection<T>`, `IEnumerable<T>`, `IList`, `ISet`, `IReadOnlyCollection<T>`.

> **Документированная неясность вокруг `IList`:** несмотря на то что сигнатура класса не включает `IList` (и .NET runtime source не содержит его реализации), страница `BitArray.IsReadOnly` на Microsoft Learn утверждает, что свойство существует потому что его требует `System.Collections.IList`. Это расхождение — inconsistency в документации Microsoft Learn, а не свидетельство скрытой реализации `IList`.

> **Замечание:** `IEnumerable` (non-generic) означает, что `GetEnumerator()` возвращает `IEnumerator` с `object`-typed `Current`. В актуальном runtime boxing overhead минимален — `BitArrayEnumeratorSimple` кэширует singleton-ы `s_boxedTrue`/`s_boxedFalse`, избегая per-element аллокаций. Ключевая проблема — отсутствие `IEnumerable<bool>`: unboxing path при `foreach` и необходимость `.Cast<bool>()` с adapter layer для LINQ.

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
| Пустой конструктор | `BitSet()` -- создаёт пустой bit set; начальная ёмкость не специфицирована публичным API (OpenJDK использует 64, но `size()` документирован как implementation-dependent) | Нет -- **обязательно указывать длину** |
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
| `Count` | Нет (read-only) | Public read-only property `BitArray`; Microsoft Learn также показывает интерфейсную карточку `ICollection.Count` (та же property, документационная проекция) |
| `Length` | **Да** (get/set) | Собственное свойство `BitArray` |

**Поведение при установке `Length`:**

| Сценарий | Поведение |
|---|---|
| `Length = N` где `N > текущего` | Расширение; новые биты инициализируются `false` |
| `Length = N` где `N < текущего` | **Усечение**; биты с индексами >= N удаляются безвозвратно |
| `Length = N` где `N < 0` | `ArgumentOutOfRangeException` |

> **Сравнение с Java:** `java.util.BitSet` не имеет аналога settable `Length`. В Java есть три размерных понятия и предикат: `size()` (implementation-dependent ёмкость; Oracle оговаривает «may change with implementation»), `length()` (logical size = max set bit + 1; уменьшается при очистке старших битов), `cardinality()` (popcount), `isEmpty()` (прямой предикат «нет set-битов»). Ёмкость (`size()`) растёт автоматически при `set()` и обычно не уменьшается, но логический размер (`length()`) отражает фактическое содержимое. В C# `Length`/`Count` -- это **фиксированный размер массива**, не зависящий от содержимого.

### Семантика размера: Java vs C#

| Понятие | Java `BitSet` | C# `BitArray` |
|---|---|---|
| Ёмкость (внутренняя) | `size()` (implementation-dependent; в OpenJDK кратна 64) | Скрыта (`byte[]`, выравнен по `sizeof(int)` = 32 бита) |
| Логический размер | `length()` (max set bit + 1) | **Нет аналога** |
| Фиксированный размер | Нет | `Length` / `Count` |
| Количество set-битов | `cardinality()` | **`PopCount()`** (с .NET 10) |
| Any/All предикаты | `isEmpty()` (any/none); прямого аналога all() нет | **`HasAnySet()`/`HasAllSet()`** (с .NET 8; возвращают `bool`) |

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
| `int PopCount()` | `int` | Количество установленных битов (cardinality) | .NET 10 |

> **Замечание:** `HasAllSet()` возвращает `true` для пустого BitArray (Length == 0) -- аналогично "for-all" квантору над пустым множеством. `PopCount()` -- прямой аналог Java `BitSet.cardinality()`, но добавлен только в .NET 10.

### 4.5. Копирование и клонирование

| Сигнатура | Возвращает | Описание |
|---|---|---|
| `object Clone()` | `object` | Shallow copy. Фактически deep copy данных, т.к. внутренний `byte[]` копируется |
| `void CopyTo(Array array, int index)` | `void` | Копирует данные в целевой массив, начиная с `index` |

**`CopyTo` поддерживает три типа целевых массивов:**

| Тип массива | Что копируется |
|---|---|
| `bool[]` | Каждый бит -> один `bool` элемент |
| `int[]` | Биты упакованы по 32 в каждый `int` |
| `byte[]` | Биты упакованы по 8 в каждый `byte` |
| Другие типы | `ArgumentException: "Only supported array types for CopyTo on BitArrays are Boolean[], Int32[] and Byte[]."` |

> **Сравнение с Java:** Java `BitSet` имеет `toByteArray()` и `toLongArray()` -- возвращают новый массив. C# `BitArray.CopyTo()` копирует в предоставленный массив, не создавая нового. Прямого `ToByteArray()` / `ToIntArray()` нет, однако с .NET 10 доступен `CollectionsMarshal.AsBytes(BitArray)` — zero-copy `Span<byte>` над байтами, необходимыми для текущего `Length` (в последнем байте возможны padding bits; неиспользованная capacity не экспонируется; нельзя менять `Length` пока span используется).

### 4.6. Итерация

| Сигнатура | Возвращает | Описание |
|---|---|---|
| `IEnumerator GetEnumerator()` | `IEnumerator` | Non-generic enumerator; `Current` — `object`-typed (runtime кэширует singleton-ы `s_boxedTrue`/`s_boxedFalse`, избегая per-element аллокаций) |

**Ключевая характеристика:** `GetEnumerator()` итерирует **все биты по порядку**, возвращая **`bool` значения** (`true`/`false`). Это _не_ итерация по индексам установленных битов.

```csharp
// Итерация возвращает bool-значения для КАЖДОГО бита
foreach (bool bit in myBitArray)  // Работает напрямую; Cast<bool>() нужен только для generic LINQ-цепочек (.Where(), .Select() и т.д.)
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

### 4.8. Interface contract mapping

`BitArray` реализует контракт `ICollection` через обычные public members:

| Контракт `ICollection` | Public member `BitArray` |
|---|---|
| `Count` | Public property `Count` (read-only) |
| `CopyTo(Array, int)` | Public method `CopyTo(Array array, int index)` |
| `IsSynchronized` | Public property `IsSynchronized` (всегда `false`) |
| `SyncRoot` | Public property `SyncRoot` |

Все четыре члена доступны напрямую через ссылку `BitArray`, без необходимости приведения к `ICollection`. Microsoft Learn дополнительно показывает отдельные интерфейсные карточки для этих же членов (e.g. `ICollection.CopyTo`, `ICollection.Count`), но в .NET runtime source (.NET 10) это обычные public members, а не explicit interface implementations — документационные карточки являются проекцией `ICollection`, а не свидетельством отдельного API-слоя.

> **Runtime-нюанс:** в implementation source (.NET 10) присутствует explicit `ISerializable.GetObjectData` и приватный десериализующий конструктор `BitArray(SerializationInfo, StreamingContext)` для обратной совместимости со старыми сериализованными данными. Оба исключены из reference assembly и не являются частью публичного API surface (подробнее — §7.6).

### 4.9. Extension methods (через IEnumerable)

Благодаря `IEnumerable` доступны LINQ extension methods:

| Метод | Описание |
|---|---|
| `Cast<TResult>()` | Приведение элементов к `bool` (`bits.Cast<bool>()`) |
| `OfType<TResult>()` | Фильтрация по типу |
| `AsParallel()` | Параллельная итерация |
| `AsQueryable()` | Конвертация в `IQueryable` |

> **Замечание:** Т.к. `BitArray` реализует non-generic `IEnumerable`, LINQ-методы вроде `.Where()`, `.Select()`, `.Count()` требуют предварительного вызова `.Cast<bool>()`, что добавляет adapter layer и unboxing path (per-element boxing аллокаций нет — runtime кэширует singleton-ы).

---

## 5. Thread Safety

- Статические (`static`) члены -- thread-safe.
- Экземплярные члены -- **не thread-safe**.
- Synchronized wrapper **не предоставляется** (в отличие от `ArrayList.Synchronized()`).
- Итерация intrinsically не thread-safe; модификация коллекции во время итерации инвалидирует enumerator — публичный контракт не гарантирует конкретный тип исключения (поведение описано как undefined), в текущей реализации это обычно `InvalidOperationException` (implementation detail).
- Для обеспечения thread safety документация рекомендует внешнюю блокировку через `SyncRoot`.

---

## 6. Полная сравнительная таблица: C# BitArray vs Java BitSet

| Категория | Java `BitSet` | C# `BitArray` |
|---|---|---|
| **Тип** | `class` (non-sealed) | `sealed class` |
| **Мутабельность** | Мутабельный | Мутабельный |
| **Immutable вариант** | Нет | Нет |
| **Размер** | Динамический (auto-grow) | Фиксированный (explicit resize через `Length`) |
| **Внутреннее представление** | `long[]` (64 бита) | `byte[]` с .NET 10 (до .NET 10 — `int[]`; выравнен по 32 бита; публичный interop через `int[]`) |
| **Интерфейсы** | `Cloneable, Serializable` | `ICollection, IEnumerable, ICloneable` |
| **Итерация: set-битов** | `stream()` -> `IntStream` | **Нет** |
| **Итерация: всех значений** | Нет | `GetEnumerator()` -> `bool` |
| **Навигация** | `nextSetBit`, `nextClearBit`, `previousSetBit`, `previousClearBit` | **Нет** |
| **Cardinality** | `cardinality()` | `PopCount()` (с .NET 10) |
| **isEmpty** | `isEmpty()` (нет set-битов) | `!HasAnySet()` (с .NET 8) или `PopCount() == 0` (.NET 10); коллекционная пустота: `Length == 0` / `Count == 0` |
| **Indexer** | Нет (`get(i)`, `set(i, v)`) | `this[int]` (`bits[i]`) |
| **Range operations** | `set(from, to)`, `clear(from, to)`, `flip(from, to)`, `get(from, to)` | **Нет** |
| **Shift** | Нет | `LeftShift(n)`, `RightShift(n)` (с .NET Core 2.0) |
| **AndNot** | `andNot(BitSet)` | **Нет** |
| **Intersects** | `intersects(BitSet)` | **Нет** |
| **Size mismatch** | Допустим | `ArgumentException` |
| **Bulk return type** | `void` | `BitArray` (= `this`, chaining) |
| **Factory methods** | `valueOf(long[])`, `valueOf(byte[])`, `valueOf(LongBuffer)`, `valueOf(ByteBuffer)` | Нет (только конструкторы) |
| **Конвертация** | `toByteArray()`, `toLongArray()` | `CopyTo(Array, int)` — единый метод, runtime dispatch по типу целевого массива (`bool[]`/`int[]`/`byte[]`); `CollectionsMarshal.AsBytes(BitArray)` → zero-copy `Span<byte>` над байтами текущего `Length` (.NET 10) |
| **Equals/HashCode** | Value-based (content) | **Reference-based (identity)** |
| **ToString** | `{0, 2, 5}` (индексы set-битов) | `"System.Collections.BitArray"` (тип) |
| **Serializable** | Да (`Serializable`) | `[Serializable]` атрибут сохранён, но `BinaryFormatter` deprecated (.NET 5+), disabled for most project types (.NET 8), removed (.NET 9: APIs throw `PlatformNotSupportedException`). `ISerializable` **не** реализуется в публичном API (в .NET 10+ есть в исходниках как implementation detail для обратной совместимости, но исключён из reference assembly) |
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
- Non-generic интерфейсы -> object-typed enumeration с unboxing path (per-element boxing аллокаций нет — runtime кэширует singleton-ы; ключевая проблема — отсутствие `IEnumerable<bool>`).
- Нет `ICollection<bool>` -> нет `Add`, `Remove`, `Contains`.
- Нет `IList<bool>` -> нет `IndexOf`, `Insert`.
- Нет `ISet<int>` -> не может представлять множество целых чисел.

### 7.4. Итерация

`GetEnumerator()` возвращает `IEnumerator` (non-generic). `Current` — `object`-typed; runtime переиспользует кэшированные singleton-ы `s_boxedTrue`/`s_boxedFalse`, избегая per-element boxing аллокаций.

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

**Выходные конвертации — единый метод `CopyTo(Array, int)` с runtime dispatch:**

Не отдельные typed overload-ы, а один метод `CopyTo(Array array, int index)` с проверкой типа целевого массива в runtime:

| Целевой тип | Семантика |
|---|---|
| `bool[]` | Каждый бит → один `bool` элемент |
| `int[]` | Биты упакованы по 32 в каждый `int` |
| `byte[]` | Биты упакованы по 8 в каждый `byte` |
| `long[]` | **Не поддерживается** |

> **Замечание:** внутреннее представление — `byte[]` (с .NET 10; до .NET 10 — `int[]`), но публичный interop (`BitArray(int[])`, `CopyTo` → `int[]`) использует `int[]`. Отсутствие `long[]` interop — историческое ограничение.

**Zero-copy byte-level view (.NET 10):**

| Сигнатура | Возвращает | Описание |
|---|---|---|
| `CollectionsMarshal.AsBytes(BitArray)` | `Span<byte>` | Zero-copy view над байтами текущего `Length` (не над всем backing `byte[]`; неиспользованная capacity не экспонируется) |

- `CollectionsMarshal` — статический helper из `System.Runtime.InteropServices`, не метод самого `BitArray`.
- Длина возвращаемого `Span<byte>` = минимальное число байт для текущего `Length`; байты неиспользованной capacity не включены. В последнем байте возможны padding bits.
- Изменение `Length` во время использования span не допускается (invalidation).
- Это первый zero-copy механизм доступа к внутренним данным `BitArray` — до .NET 10 единственным способом был copy-based `CopyTo`.

**Binary serialization:**

| Слой | Поведение |
|---|---|
| Публичный API surface (reference assembly) | `[Serializable]` атрибут сохранён; `ISerializable` **не** экспонируется |
| Runtime source (.NET 10) | Explicit `ISerializable.GetObjectData` + приватный десериализующий конструктор `BitArray(SerializationInfo, StreamingContext)` — сериализуют/десериализуют payload через `int[]` для обратной совместимости со старыми `BinaryFormatter`-данными |

> **Контекст:** `BinaryFormatter` deprecated (.NET 5+), disabled for most project types (.NET 8), APIs throw `PlatformNotSupportedException` (.NET 9). Runtime-`ISerializable` path в .NET 10 не делает `BitArray` рекомендуемым сериализационным типом — это исключительно compatibility behavior для десериализации ранее сохранённых данных. Для нового кода рекомендуется ручной export через `CopyTo` или `CollectionsMarshal.AsBytes`.

---

## 8. Хронология эволюции API

| Версия .NET | Добавлено | Комментарий |
|---|---|---|
| .NET Framework 1.0 | Core API: Get, Set, SetAll, And, Or, Xor, Not, Clone, CopyTo, GetEnumerator, Length, Count, Item[] | Первоначальный дизайн |
| .NET Core 2.0 / .NET Standard 2.1 | `LeftShift(int)`, `RightShift(int)` | Shift-операции, in-place, возвращают `this` |
| .NET 8 | `HasAllSet()`, `HasAnySet()` | Быстрые проверки наличия/отсутствия set-битов |
| .NET 10 | `PopCount()`, переход `int[] → byte[]`, `CollectionsMarshal.AsBytes(BitArray)`, runtime `ISerializable` | `PopCount()` — аналог `cardinality()` из Java. Внутреннее представление изменено с `int[]` на `byte[]`; одновременно добавлен `CollectionsMarshal.AsBytes(BitArray)` — zero-copy `Span<byte>` view над байтами текущего `Length` (не вся capacity). В runtime source добавлена explicit реализация `ISerializable` (не в reference assembly) для обратной совместимости со старыми `BinaryFormatter`-данными |

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
| Тип | `sealed class` (reference type) | `struct` (value type) |
| Размещение | Всегда в managed heap | В типичных unboxed сценариях без отдельной heap-аллокации (но не `ref struct` — boxing возможен) |
| Размер | Произвольный | **Ровно 32 бита** |
| Интерфейсы | `ICollection, IEnumerable, ICloneable` | `IEquatable<BitVector32>` |
| Использование | Общего назначения | Bit flags и packed integers |
| Производительность | Аллокация + GC | Lightweight value type (32 бита inline) |
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

`BitVector32` поддерживает **два способа адресации** — bit flags (по маскам) и sections (packed integers). Microsoft предупреждает, что mask-based и section-based доступ не следует смешивать на одном layout, но section-based layout допускает `CreateSection(maxValue: 1)` для кодирования булевых флагов наряду с целочисленными секциями:

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

`BitVector32` -- специализированный value type для performance-critical кода, работающего ровно с 32 битами. Не является альтернативой `BitArray` для общих задач, скорее дополняет его для сценариев, где известно, что битов <= 32. Как обычный `struct` (не `ref struct`), `BitVector32` в типичных unboxed сценариях обходится без отдельной heap-аллокации, но не имеет гарантии stack-only размещения. Два способа адресации (bit flags по маскам / packed integers по секциям; смешивать оба на одном layout не рекомендуется, но section-based layout поддерживает `Section(maxValue: 1)` для булевых значений наряду с целочисленными) -- уникальная особенность, не встречающаяся в BitSet-API других языков.

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
6. **Нет `Intersects`** — проверка пересечения требует временной копии: `new BitArray(a).And(b).HasAnySet()`, т.к. `And()` модифицирует `this` in-place. Это дополнительная аллокация и копирование, плюс сохраняется требование равенства длин.
7. **Нет iteration by set-bit indices** -- `GetEnumerator` возвращает `bool`-значения, не индексы. Для sparse BitArray -- неприемлемая производительность.
8. **Strict size matching для bulk ops** -- `ArgumentException` при разных длинах. Java допускает разные размеры и это удобнее.
9. **Non-generic `IEnumerable`** -- object-typed enumeration с unboxing path; отсутствие `IEnumerable<bool>` — design-level проблема (per-element boxing аллокаций нет благодаря кэшированным singleton-ам в runtime).
10. **`byte[]` внутреннее хранилище** -- storage `byte[]` (с .NET 10; до .NET 10 — `int[]`), выровнен по `sizeof(int)`. Bulk-операции (And/Or/Xor/Not) векторизованы: основной path использует `Vector512`/`Vector256`/`Vector128` поверх `byte[]`, remainder/shift paths — `int`-span через `MemoryMarshal.Cast<byte, int>`. Java `BitSet` использует `long[]`, что упрощает scalar fallback на 64-битных платформах, но при наличии векторизации разница в storage granularity менее значима.
11. **Serialization ограничена** — `[Serializable]` атрибут сохранён, но `ISerializable` не является частью публичного API. `BinaryFormatter` deprecated (.NET 5+), disabled for most project types (.NET 8), removed (.NET 9) — практически недоступна на современных версиях .NET.
12. **Чрезвычайно медленная эволюция API** -- за 25 лет добавлено 5 методов. `PopCount()` (аналог `cardinality()`) появился только в .NET 10.

### Открытые вопросы

1. **Settable Length** -- стоит ли Kotlin BitSet поддерживать resize? Java -- auto-grow, C# -- explicit resize. Kotlin может выбрать третий путь (immutable size + builder/factory для resize).
2. **Итерация: `bool`-значения vs индексы set-битов** -- C# выбрал первое, Java -- второе. Для Kotlin стоит рассмотреть оба: `Iterable<Int>` (индексы) + `forEachBit {}` (как в BitSetUtil.kt).
3. **Return type bulk операций** -- `void` (Java) vs `this` (C#). Kotlin может вернуть `Unit` для мутабельного варианта и новый объект для immutable.
