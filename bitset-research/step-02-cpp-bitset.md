# C++ BitSet Implementations — Cross-Language API Analysis

## Резюме

Проведён детальный анализ двух C++ реализаций BitSet: `std::bitset<N>` (стандартная библиотека) и `boost::dynamic_bitset<Block, Allocator>`. Они представляют два фундаментально разных подхода к размеру: compile-time фиксированный (`std::bitset`) vs runtime динамический (`boost::dynamic_bitset`). Обе реализации мутабельные, не реализуют стандартные интерфейсы контейнеров и не предоставляют итераторов. `boost::dynamic_bitset` расширяет API `std::bitset` значительно: добавляет динамическое управление размером (`resize`, `push_back`, `append`, `clear`, `reserve`, `capacity`), set-теоретические операции (`is_subset_of`, `is_proper_subset_of`, `intersects`), set difference (`operator-`), полное упорядочение (`<`, `<=`, `>`, `>=`), навигацию по set-битам (`find_first`, `find_next`), range-версии `set`/`reset`/`flip`, и блочный ввод-вывод (`to_block_range`, `from_block_range`). `std::bitset` компенсирует простотой и constexpr-поддержкой (с C++23).

---

## 1. `std::bitset<N>` (C++ Standard Library)

**Заголовочный файл:** `<bitset>`
**Стандарт:** C++98 (с расширениями в C++11, C++20, C++23, C++26)

### 1.1 Объявление

```cpp
template<std::size_t N>
class bitset;
```

**Параметр шаблона:**
- `N` — количество бит, задаётся на этапе компиляции. Не может быть изменено после создания объекта.

### 1.2 Вложенные типы

| Тип | Описание |
|---|---|
| `reference` | Proxy-класс, возвращаемый из `operator[]` для неконстантного доступа к биту |

### 1.3 Конструкторы

| Сигнатура | Описание | Стандарт |
|---|---|---|
| `constexpr bitset()` | Все биты инициализируются нулями | C++98 (constexpr с C++23) |
| `constexpr bitset(unsigned long val)` | Инициализация из `unsigned long`; биты выше `min(N, digits)` обнуляются | C++98 (constexpr с C++23) |
| `constexpr bitset(unsigned long long val)` | Инициализация из `unsigned long long` | C++11 (constexpr с C++23) |
| `explicit constexpr bitset(const std::string& s, size_t pos = 0, size_t count = npos, char zero = '0', char one = '1')` | Инициализация из строки символов `'0'`/`'1'` | C++98 (constexpr с C++23) |
| `explicit constexpr bitset(const char* s, size_t count = npos, char zero = '0', char one = '1')` | Инициализация из C-строки | C++98 (constexpr с C++23) |
| `explicit constexpr bitset(std::string_view s, size_t pos = 0, size_t count = npos, char zero = '0', char one = '1')` | Инициализация из `string_view` | C++26 |

### 1.4 Доступ к элементам

| Сигнатура | Описание |
|---|---|
| `constexpr reference operator[](size_t pos)` | Proxy-ссылка на бит (чтение/запись), без проверки границ |
| `constexpr bool operator[](size_t pos) const` | Значение бита, без проверки границ |
| `constexpr bool test(size_t pos) const` | Значение бита с проверкой границ; бросает `std::out_of_range` |

### 1.5 Bulk-запросы

| Сигнатура | Описание | Стандарт |
|---|---|---|
| `constexpr bool all() const` | `true` если все биты установлены | C++11 |
| `constexpr bool any() const` | `true` если хотя бы один бит установлен | C++98 |
| `constexpr bool none() const` | `true` если ни один бит не установлен | C++98 |
| `constexpr size_t count() const` | Количество установленных битов (popcount) | C++98 |
| `constexpr size_t size() const` | Возвращает `N` (количество бит) | C++98 |

### 1.6 Мутация

| Сигнатура | Описание |
|---|---|
| `constexpr bitset& set()` | Установить все биты в 1 |
| `constexpr bitset& set(size_t pos, bool value = true)` | Установить конкретный бит; бросает `std::out_of_range` |
| `constexpr bitset& reset()` | Сбросить все биты в 0 |
| `constexpr bitset& reset(size_t pos)` | Сбросить конкретный бит; бросает `std::out_of_range` |
| `constexpr bitset& flip()` | Инвертировать все биты |
| `constexpr bitset& flip(size_t pos)` | Инвертировать конкретный бит; бросает `std::out_of_range` |

**Примечание:** в `std::bitset` отсутствуют range-версии `set`/`reset`/`flip` — невозможно установить/сбросить/инвертировать диапазон битов одним вызовом.

### 1.7 Побитовые операторы (члены класса)

| Сигнатура | Описание |
|---|---|
| `constexpr bitset& operator&=(const bitset& other)` | Побитовое AND с присвоением |
| `constexpr bitset& operator\|=(const bitset& other)` | Побитовое OR с присвоением |
| `constexpr bitset& operator^=(const bitset& other)` | Побитовое XOR с присвоением |
| `constexpr bitset operator~() const` | Побитовое NOT (возвращает копию) |
| `constexpr bitset& operator<<=(size_t pos)` | Сдвиг влево с присвоением |
| `constexpr bitset& operator>>=(size_t pos)` | Сдвиг вправо с присвоением |
| `constexpr bitset operator<<(size_t pos) const` | Сдвиг влево (возвращает копию) |
| `constexpr bitset operator>>(size_t pos) const` | Сдвиг вправо (возвращает копию) |

### 1.8 Сравнение

| Сигнатура | Описание | Стандарт |
|---|---|---|
| `constexpr bool operator==(const bitset& other) const` | Равенство | C++98 (семантика обновлена в C++20) |
| `constexpr bool operator!=(const bitset& other) const` | Неравенство | C++98 (удалён в C++20, генерируется автоматически) |

**Примечание:** `std::bitset` не поддерживает операторы `<`, `<=`, `>`, `>=` — нет упорядочения.

### 1.9 Конвертации

| Сигнатура | Описание | Стандарт |
|---|---|---|
| `template<...> std::basic_string<CharT, Traits, Allocator> to_string(CharT zero = '0', CharT one = '1') const` | Конвертация в строку `'0'`/`'1'` | C++98 |
| `unsigned long to_ulong() const` | Конвертация в `unsigned long`; бросает `std::overflow_error` если не помещается | C++98 |
| `unsigned long long to_ullong() const` | Конвертация в `unsigned long long`; бросает `std::overflow_error` если не помещается | C++11 |

### 1.10 Не-членские функции

| Сигнатура | Описание |
|---|---|
| `template<size_t N> bitset<N> operator&(const bitset<N>& lhs, const bitset<N>& rhs)` | Побитовое AND |
| `template<size_t N> bitset<N> operator\|(const bitset<N>& lhs, const bitset<N>& rhs)` | Побитовое OR |
| `template<size_t N> bitset<N> operator^(const bitset<N>& lhs, const bitset<N>& rhs)` | Побитовое XOR |
| `template<...> ostream& operator<<(ostream& os, const bitset<N>& x)` | Потоковый вывод |
| `template<...> istream& operator>>(istream& is, bitset<N>& x)` | Потоковый ввод |

### 1.11 Специализация `std::hash` (C++11)

```cpp
template<std::size_t N>
struct std::hash<std::bitset<N>>;
```

Поддерживает использование в `std::unordered_set` и `std::unordered_map`.

### 1.12 Вложенный класс `reference`

```cpp
class reference {
    constexpr reference& operator=(bool x);          // Присвоение из bool
    constexpr reference& operator=(const reference&); // Присвоение из другой ссылки
    constexpr operator bool() const;                  // Конвертация в bool
    constexpr bool operator~() const;                 // Инвертированное значение
    constexpr reference& flip();                      // Инвертировать бит на месте
};
```

### 1.13 Feature-Test Macros

| Макрос | Значение | Стандарт | Назначение |
|---|---|---|---|
| `__cpp_lib_constexpr_bitset` | `202207L` | C++23 | Полная constexpr-поддержка |
| `__cpp_lib_bitset` | `202306L` | C++26 | Интеграция со `std::string_view` |

---

## 2. `boost::dynamic_bitset<Block, Allocator>`

**Заголовочный файл:** `boost/dynamic_bitset.hpp` (forward declaration в `boost/dynamic_bitset_fwd.hpp`)
**Авторы:** Jeremy Siek, Gennaro Prota, Ahmed Charles, и др.

### 2.1 Объявление

```cpp
namespace boost {

template <typename Block = unsigned long,
          typename Allocator = std::allocator<Block>>
class dynamic_bitset;

}
```

**Параметры шаблона:**

| Параметр | Описание | Значение по умолчанию |
|---|---|---|
| `Block` | Unsigned integer тип для хранения бит. Определяет гранулярность внутреннего хранилища | `unsigned long` |
| `Allocator` | Аллокатор для внутреннего управления памятью | `std::allocator<Block>` |

**Требование:** `Block` должен быть беззнаковым целым типом. `Allocator` должен удовлетворять требованиям стандартного аллокатора.

### 2.2 Моделируемые концепты

`Assignable`, `DefaultConstructible`, `EqualityComparable`, `LessThanComparable`.

Явно **не** моделирует `Container` — по причине proxy-типа `reference`, который не удовлетворяет требованиям ForwardIterator (аналогичная проблема с `std::vector<bool>`).

### 2.3 Вложенные типы

| Тип | Описание |
|---|---|
| `block_type` | Синоним `Block` |
| `allocator_type` | Синоним `Allocator` |
| `size_type` | Unsigned integer тип для представления размера |
| `reference` | Proxy-класс для неконстантного доступа к биту |
| `const_reference` | `bool` |

### 2.4 Статические данные

| Имя | Тип | Описание |
|---|---|---|
| `bits_per_block` | `int` | `std::numeric_limits<Block>::digits` |
| `npos` | `size_type` | Максимальное значение `size_type`, используется как sentinel в `find_first`/`find_next` |

### 2.5 Вложенный класс `reference`

В отличие от `std::bitset::reference`, поддерживает расширенный набор операторов:

```cpp
class reference {
    reference& operator=(bool value);            // Присвоение из bool
    reference& operator=(const reference& rhs);  // Присвоение из другой ссылки
    reference& operator|=(bool value);           // OR с присвоением
    reference& operator&=(bool value);           // AND с присвоением
    reference& operator^=(bool value);           // XOR с присвоением
    reference& operator-=(bool value);           // Set difference с присвоением
    bool operator~() const;                      // Инвертированное значение
    operator bool() const;                       // Конвертация в bool
    reference& flip();                           // Инвертировать бит на месте
};
```

### 2.6 Конструкторы

| Сигнатура | Описание |
|---|---|
| `dynamic_bitset()` | Пустой bitset размера 0, аллокатор по умолчанию |
| `explicit dynamic_bitset(const Allocator& alloc)` | Пустой bitset размера 0 с указанным аллокатором |
| `explicit dynamic_bitset(size_type num_bits, unsigned long value = 0, const Allocator& alloc = Allocator())` | Bitset из `num_bits` бит; первые `min(num_bits, digits_of_ulong)` бит инициализируются из `value`, остальные — нулями |
| `template<typename CharT, typename Traits, typename Alloc> explicit dynamic_bitset(const basic_string<CharT, Traits, Alloc>& s, size_type pos = 0, size_type n = npos, const Allocator& alloc = Allocator())` | Инициализация из строки символов `'0'`/`'1'`; высший символ соответствует младшему биту (как в `std::bitset`) |
| `template<typename BlockInputIterator> dynamic_bitset(BlockInputIterator first, BlockInputIterator last, const Allocator& alloc = Allocator())` | Инициализация из диапазона блоков; блок 0 — биты `[0, bits_per_block)`, блок 1 — `[bits_per_block, 2*bits_per_block)`, и т.д. |
| `dynamic_bitset(const dynamic_bitset& b)` | Копирующий конструктор |
| `dynamic_bitset(dynamic_bitset&& b)` | Перемещающий конструктор (Boost 1.56+) |

### 2.7 Присвоение и обмен

| Сигнатура | Описание |
|---|---|
| `dynamic_bitset& operator=(const dynamic_bitset& b)` | Копирующее присвоение |
| `dynamic_bitset& operator=(dynamic_bitset&& b)` | Перемещающее присвоение (Boost 1.56+) |
| `void swap(dynamic_bitset& b)` | Обмен содержимого; noexcept |
| `allocator_type get_allocator() const` | Возвращает копию аллокатора |

### 2.8 Управление размером (уникально для dynamic_bitset)

| Сигнатура | Описание |
|---|---|
| `void resize(size_type num_bits, bool value = false)` | Изменить количество бит. Если растёт — новые биты = `value`; если уменьшается — старшие биты отбрасываются |
| `void clear()` | Размер становится 0 |
| `void push_back(bool bit)` | Добавить бит в позицию MSB; увеличивает `size()` на 1 |
| `void pop_back()` | Удалить MSB; уменьшает `size()` на 1. Предусловие: `!empty()` |
| `void append(Block block)` | Добавить целый блок бит к MSB-концу; увеличивает `size()` на `bits_per_block` |
| `template<typename BlockInputIterator> void append(BlockInputIterator first, BlockInputIterator last)` | Добавить диапазон блоков |

### 2.9 Ёмкость

| Сигнатура | Описание |
|---|---|
| `size_type size() const noexcept` | Количество бит |
| `size_type num_blocks() const noexcept` | Количество блоков |
| `size_type max_size() const noexcept` | Максимально возможный размер |
| `bool empty() const noexcept` | `true` если `size() == 0` (не путать с `none()`) |
| `size_type capacity() const noexcept` | Текущая ёмкость без реаллокации |
| `void reserve(size_type num_bits)` | Зарезервировать память для `num_bits` бит без изменения `size()` |
| `void shrink_to_fit()` | Запрос на уменьшение неиспользуемой памяти без изменения `size()` |

### 2.10 Доступ к элементам

| Сигнатура | Описание |
|---|---|
| `reference operator[](size_type n)` | Proxy-ссылка на бит; без проверки границ |
| `bool operator[](size_type n) const` | Значение бита; без проверки границ |
| `reference at(size_type n)` | Proxy-ссылка с проверкой границ; бросает `std::out_of_range` |
| `bool at(size_type n) const` | Значение бита с проверкой границ; бросает `std::out_of_range` |
| `bool test(size_type n) const` | Значение бита (без исключений, предусловие: `n < size()`) |
| `bool test_set(size_type n, bool val = true)` | Установить бит и вернуть **предыдущее** значение |

### 2.11 Мутация одиночных бит и диапазонов

| Сигнатура | Описание |
|---|---|
| `dynamic_bitset& set()` | Установить все биты в 1 |
| `dynamic_bitset& set(size_type n, bool val = true)` | Установить/сбросить один бит |
| `dynamic_bitset& set(size_type n, size_type len, bool val)` | Установить/сбросить диапазон `[n, n+len)` |
| `dynamic_bitset& reset()` | Сбросить все биты в 0 |
| `dynamic_bitset& reset(size_type n)` | Сбросить один бит |
| `dynamic_bitset& reset(size_type n, size_type len)` | Сбросить диапазон `[n, n+len)` |
| `dynamic_bitset& flip()` | Инвертировать все биты |
| `dynamic_bitset& flip(size_type n)` | Инвертировать один бит |
| `dynamic_bitset& flip(size_type n, size_type len)` | Инвертировать диапазон `[n, n+len)` |

### 2.12 Bulk-запросы

| Сигнатура | Описание |
|---|---|
| `bool all() const` | `true` если все биты установлены (или `size() == 0`) |
| `bool any() const` | `true` если хотя бы один бит установлен |
| `bool none() const` | `true` если ни один бит не установлен |
| `size_type count() const noexcept` | Количество установленных бит (popcount) |

### 2.13 Побитовые операторы (члены класса)

| Сигнатура | Описание |
|---|---|
| `dynamic_bitset& operator&=(const dynamic_bitset& rhs)` | Побитовое AND с присвоением. Требует `size() == rhs.size()` |
| `dynamic_bitset& operator\|=(const dynamic_bitset& rhs)` | Побитовое OR с присвоением. Требует `size() == rhs.size()` |
| `dynamic_bitset& operator^=(const dynamic_bitset& rhs)` | Побитовое XOR с присвоением. Требует `size() == rhs.size()` |
| `dynamic_bitset& operator-=(const dynamic_bitset& rhs)` | Set difference с присвоением (`a &= ~b`). Требует `size() == rhs.size()` |
| `dynamic_bitset operator~() const` | Побитовое NOT (возвращает копию) |
| `dynamic_bitset& operator<<=(size_type n)` | Сдвиг влево с присвоением |
| `dynamic_bitset& operator>>=(size_type n)` | Сдвиг вправо с присвоением |
| `dynamic_bitset operator<<(size_type n) const` | Сдвиг влево (возвращает копию) |
| `dynamic_bitset operator>>(size_type n) const` | Сдвиг вправо (возвращает копию) |

### 2.14 Set-теоретические операции (уникально для dynamic_bitset)

| Сигнатура | Описание |
|---|---|
| `bool is_subset_of(const dynamic_bitset& a) const` | `true` если каждый set-бит `*this` также set в `a`. Требует `size() == a.size()` |
| `bool is_proper_subset_of(const dynamic_bitset& a) const` | `true` если `is_subset_of(a) && count() < a.count()`. Требует `size() == a.size()` |
| `bool intersects(const dynamic_bitset& a) const` | `true` если существует бит, установленный в обоих bitset. Требует `size() == a.size()` |

### 2.15 Навигация по set-битам

| Сигнатура | Описание |
|---|---|
| `size_type find_first() const` | Индекс младшего set-бита, или `npos` если нет set-битов |
| `size_type find_next(size_type pos) const` | Индекс следующего set-бита строго после `pos`, или `npos` |

Паттерн итерации:
```cpp
for (auto i = b.find_first(); i != boost::dynamic_bitset<>::npos; i = b.find_next(i)) {
    // process bit i
}
```

### 2.16 Сравнение

| Сигнатура | Описание |
|---|---|
| `bool operator==(const dynamic_bitset& rhs) const` | Побитовое равенство; `true` только если `size() == rhs.size()` и все биты совпадают |
| `bool operator!=(const dynamic_bitset& rhs) const` | Неравенство |
| `bool operator<(const dynamic_bitset& rhs) const` | Лексикографический порядок |
| `bool operator<=(const dynamic_bitset& rhs) const` | `<=` по лексикографическому порядку |
| `bool operator>(const dynamic_bitset& rhs) const` | `>` по лексикографическому порядку |
| `bool operator>=(const dynamic_bitset& rhs) const` | `>=` по лексикографическому порядку |

### 2.17 Конвертация

| Сигнатура | Описание |
|---|---|
| `unsigned long to_ulong() const` | Конвертация в `unsigned long`; бросает `std::overflow_error` если не помещается |

**Примечание:** `to_ullong()` отсутствует в `boost::dynamic_bitset` (в отличие от `std::bitset`).

### 2.18 Не-членские функции

#### Побитовые операторы

| Сигнатура | Описание |
|---|---|
| `dynamic_bitset operator&(const dynamic_bitset& a, const dynamic_bitset& b)` | Побитовое AND. Требует `a.size() == b.size()` |
| `dynamic_bitset operator\|(const dynamic_bitset& a, const dynamic_bitset& b)` | Побитовое OR. Требует `a.size() == b.size()` |
| `dynamic_bitset operator^(const dynamic_bitset& a, const dynamic_bitset& b)` | Побитовое XOR. Требует `a.size() == b.size()` |
| `dynamic_bitset operator-(const dynamic_bitset& a, const dynamic_bitset& b)` | Set difference. Требует `a.size() == b.size()` |

#### Сериализация и interop

| Сигнатура | Описание |
|---|---|
| `template<...> void to_string(const dynamic_bitset& b, basic_string<CharT, Alloc>& s)` | Конвертация в строку `'0'`/`'1'` (не-членская для гибкости шаблонных параметров) |
| `template<...> void to_block_range(const dynamic_bitset& b, BlockOutputIterator result)` | Запись бит в итератор блоков (block 0 = биты `[0, bits_per_block)`) |
| `template<...> void from_block_range(BlockIterator first, BlockIterator last, dynamic_bitset& b)` | Чтение блоков в bitset |

#### Потоковый ввод-вывод

| Сигнатура | Описание |
|---|---|
| `template<...> basic_ostream& operator<<(basic_ostream& os, const dynamic_bitset& b)` | Потоковый вывод (MSB first); учитывает locale |
| `template<...> basic_istream& operator>>(basic_istream& is, dynamic_bitset& b)` | Потоковый ввод; динамически расширяет bitset по мере чтения символов |

---

## 3. Сравнительный анализ `std::bitset` vs `boost::dynamic_bitset`

### 3.1 Полная таблица API

| Операция | `std::bitset<N>` | `boost::dynamic_bitset` | Комментарий |
|---|---|---|---|
| **Конструкторы** | | | |
| Default | `bitset()` | `dynamic_bitset()` | std: N бит (все 0); boost: 0 бит |
| From integer | `bitset(unsigned long long)` | `dynamic_bitset(size_type, unsigned long)` | boost требует указать size |
| From string | `bitset(string, pos, count, zero, one)` | `dynamic_bitset(string, pos, n, alloc)` | Оба поддерживают `'0'`/`'1'` |
| From string_view | `bitset(string_view)` (C++26) | -- | Только std |
| From block range | -- | `dynamic_bitset(first, last)` | Только boost |
| Copy | Implicit | `dynamic_bitset(const dynamic_bitset&)` | |
| Move | Implicit | `dynamic_bitset(dynamic_bitset&&)` | boost: Boost 1.56+ |
| With allocator | -- | `dynamic_bitset(const Allocator&)` | Только boost |
| **Управление размером** | | | |
| `resize` | -- | `resize(num_bits, value)` | |
| `clear` | -- | `clear()` | Размер → 0 |
| `push_back` | -- | `push_back(bool)` | |
| `pop_back` | -- | `pop_back()` | |
| `append` (block) | -- | `append(Block)` | |
| `append` (range) | -- | `append(first, last)` | |
| `reserve` | -- | `reserve(num_bits)` | |
| `capacity` | -- | `capacity()` | |
| `shrink_to_fit` | -- | `shrink_to_fit()` | |
| **Ёмкость/размер** | | | |
| `size` | `size()` = N | `size()` = текущее кол-во бит | |
| `num_blocks` | -- | `num_blocks()` | |
| `max_size` | -- | `max_size()` | |
| `empty` | -- | `empty()` | `size() == 0` |
| **Доступ к элементам** | | | |
| `operator[]` | Да (reference/bool) | Да (reference/bool) | |
| `at` | -- | `at(n)` (reference/bool) | Бросает `std::out_of_range` |
| `test` | `test(pos)` — бросает out_of_range | `test(n)` — нет исключений (assert) | Разная семантика ошибок! |
| `test_set` | -- | `test_set(n, val)` | Атомарный get+set |
| **Мутация: одиночный бит** | | | |
| `set(pos)` | Да | Да | |
| `set(pos, val)` | Да | Да | |
| `reset(pos)` | Да | Да | |
| `flip(pos)` | Да | Да | |
| **Мутация: все биты** | | | |
| `set()` | Да | Да | |
| `reset()` | Да | Да | |
| `flip()` | Да | Да | |
| **Мутация: диапазон** | | | |
| `set(n, len, val)` | -- | Да | |
| `reset(n, len)` | -- | Да | |
| `flip(n, len)` | -- | Да | |
| **Bulk-запросы** | | | |
| `all` | Да (C++11) | Да | |
| `any` | Да | Да | |
| `none` | Да | Да | |
| `count` | Да | Да | |
| **Побитовые операторы** | | | |
| `&=`, `\|=`, `^=` | Да | Да | boost: requires equal size |
| `-=` (set diff) | -- | Да | Только boost |
| `~` | Да | Да | |
| `<<=`, `>>=` | Да | Да | |
| `<<`, `>>` | Да | Да | |
| `&`, `\|`, `^` (non-member) | Да | Да | |
| `-` (non-member) | -- | Да | Только boost |
| **Set-теоретические** | | | |
| `is_subset_of` | -- | Да | |
| `is_proper_subset_of` | -- | Да | |
| `intersects` | -- | Да | |
| **Навигация по set-битам** | | | |
| `find_first` | -- | Да | Возвращает `npos` если нет |
| `find_next` | -- | Да | Возвращает `npos` если нет |
| **Сравнение** | | | |
| `==`, `!=` | Да | Да | |
| `<`, `<=`, `>`, `>=` | -- | Да | Лексикографический порядок |
| **Конвертации** | | | |
| `to_string` | Да (member) | Да (non-member) | |
| `to_ulong` | Да | Да | |
| `to_ullong` | Да (C++11) | -- | |
| `to_block_range` | -- | Да | |
| `from_block_range` | -- | Да | |
| **I/O** | | | |
| `operator<<` (stream) | Да | Да | |
| `operator>>` (stream) | Да | Да (динамически расширяет) | |
| **Прочее** | | | |
| `swap` | -- | `swap(dynamic_bitset&)` | |
| `get_allocator` | -- | Да | |
| `std::hash` | Да (C++11) | -- | |

### 3.2 Анализ по осям сравнения

#### 3.2.1 Мутабельность

| Аспект | `std::bitset<N>` | `boost::dynamic_bitset` |
|---|---|---|
| Мутабельный? | Да | Да |
| Const-корректность | Да — `const` версии `operator[]`, `test`, `count`, `all`, `any`, `none`, `size`, `to_string`, `to_ulong` | Да — аналогичный набор `const` методов |
| Immutable вариант? | Нет | Нет |

Обе реализации полностью мутабельные. Const-корректность обеспечивается через const overloads методов. Ни одна не предоставляет отдельного immutable типа или read-only view.

#### 3.2.2 Модель размера

| Аспект | `std::bitset<N>` | `boost::dynamic_bitset` |
|---|---|---|
| Когда задаётся размер | Compile-time (параметр шаблона `N`) | Runtime (аргумент конструктора) |
| Изменяем ли | Нет, никогда | Да: `resize`, `push_back`, `pop_back`, `append`, `clear` |
| Авто-рост | Нет | Нет — требуется явный вызов `resize`/`push_back`/`append` |
| Управление ёмкостью | Нет (фиксировано) | `reserve`, `capacity`, `shrink_to_fit` — аналогично `std::vector` |
| `empty()` | Нет (не имеет смысла при N > 0) | Да — `size() == 0` |

Ключевое отличие: `std::bitset<N>` — это value type полностью на стеке (для разумных N), а `boost::dynamic_bitset` управляет heap-allocated хранилищем через аллокатор.

`boost::dynamic_bitset` **не** поддерживает автоматический рост при записи за пределы текущего размера (в отличие от `java.util.BitSet`). Попытка `set(pos)` при `pos >= size()` — нарушение предусловия (assert).

#### 3.2.3 Интерфейсы коллекций

| Аспект | `std::bitset<N>` | `boost::dynamic_bitset` |
|---|---|---|
| Container | Нет | Нет (явно отвергнуто в документации) |
| Iterable | Нет | Нет |
| ForwardIterator | Нет | Нет |
| Причина | Не рассматривался как контейнер в стандарте | Proxy `reference` не удовлетворяет ForwardIterator (та же проблема, что у `vector<bool>`) |

Документация `boost::dynamic_bitset` явно объясняет решение не реализовывать Container: proxy-тип `reference` нарушает требования ForwardIterator, что ведёт к subtle bugs при использовании со стандартными алгоритмами (как у `std::vector<bool>`). Авторы предпочли честное API вместо ложного соответствия концепту Container.

#### 3.2.4 Поддержка итерации

| Аспект | `std::bitset<N>` | `boost::dynamic_bitset` |
|---|---|---|
| Итераторы begin/end | Нет | Нет |
| Range-based for | Нет | Нет |
| Итерация по set-битам | Только вручную: `for (i = 0; i < N; ++i) if (bs.test(i)) ...` | `find_first()` + `find_next(pos)` |
| Итерация по всем битам | Ручной цикл по индексам | Ручной цикл по индексам |
| Итерация по clear-битам | Только вручную | Только вручную |

`boost::dynamic_bitset` предоставляет эффективный механизм итерации по set-битам через `find_first()`/`find_next()`, который может пропускать целые блоки нулей. В `std::bitset` такого механизма нет — приходится проверять каждый бит по отдельности.

#### 3.2.5 Операторы и синтаксический сахар

| Оператор | `std::bitset<N>` | `boost::dynamic_bitset` | Семантика |
|---|---|---|---|
| `[]` | Да | Да | Доступ к биту (proxy reference / bool) |
| `&=` | Да | Да | Побитовое AND |
| `\|=` | Да | Да | Побитовое OR |
| `^=` | Да | Да | Побитовое XOR |
| `-=` | Нет | Да | Set difference (AND NOT) |
| `~` | Да | Да | Побитовое NOT (копия) |
| `<<=` | Да | Да | Сдвиг влево |
| `>>=` | Да | Да | Сдвиг вправо |
| `<<` | Да | Да | Сдвиг влево (копия) |
| `>>` | Да | Да | Сдвиг вправо (копия) |
| `==` | Да | Да | Равенство |
| `!=` | Да (C++20: auto) | Да | Неравенство |
| `<` | Нет | Да | Лексикографический порядок |
| `<=` | Нет | Да | |
| `>` | Нет | Да | |
| `>=` | Нет | Да | |
| `<<` (stream) | Да | Да | Потоковый вывод |
| `>>` (stream) | Да | Да | Потоковый ввод |

`boost::dynamic_bitset` богаче: добавлены `operator-=` / `operator-` для set difference и полный набор операторов упорядочения.

#### 3.2.6 Сериализация и interop

| Аспект | `std::bitset<N>` | `boost::dynamic_bitset` |
|---|---|---|
| В строку | `to_string()` (member, шаблонный) | `to_string(b, s)` (non-member) |
| Из строки | Конструктор из `string`/`string_view` | Конструктор из `basic_string` |
| В unsigned long | `to_ulong()` | `to_ulong()` |
| В unsigned long long | `to_ullong()` (C++11) | -- |
| В блоки | -- | `to_block_range(b, output_iter)` |
| Из блоков | -- | Конструктор из `BlockInputIterator` + `from_block_range()` |
| В byte[] | -- | -- (через `to_block_range` с `Block = uint8_t`) |
| `std::hash` | Да (C++11) | Нет |
| Stream I/O | Да | Да |

Ключевое отличие: `boost::dynamic_bitset` предоставляет block-level interop (`to_block_range`/`from_block_range`) для эффективной сериализации и interop с другими представлениями. Это позволяет конвертировать bitset в/из `vector<Block>`, файла и т.д. без посимвольной обработки.

`std::bitset` ограничен конвертацией в скалярные типы (`unsigned long`, `unsigned long long`) и строки. Нет механизма для получения внутреннего представления побитово или поблочно.

---

## 4. Ключевые наблюдения для дизайна Kotlin BitSet

### 4.1 Что C++ делает хорошо

1. **Два уровня абстракции** — compile-time фиксированный (`std::bitset`) и runtime динамический (`boost::dynamic_bitset`) покрывают разные use cases. Для Kotlin stdlib наиболее релевантен динамический вариант.

2. **Полный набор побитовых операторов** — оба типа перегружают `&`, `|`, `^`, `~`, `<<`, `>>` как операторы, обеспечивая естественный синтаксис для битовых манипуляций.

3. **Set difference как оператор (`-`)** — `boost::dynamic_bitset` определяет `operator-` для `a & ~b`, что повышает читаемость set-теоретических операций.

4. **Block-level interop** — `to_block_range`/`from_block_range` позволяют эффективно сериализовать и десериализовать bitset без промежуточных строковых представлений.

5. **Навигация `find_first`/`find_next`** — эффективная итерация по set-битам без проверки каждого бита по отдельности.

6. **Set-теоретические предикаты** — `is_subset_of`, `is_proper_subset_of`, `intersects` позволяют отвечать на вопросы о множествах без создания промежуточных bitset.

7. **`test_set`** — атомарный get+set, полезен для паттерна "посетить каждый бит ровно один раз".

8. **Range-версии `set`/`reset`/`flip`** в `boost::dynamic_bitset` — позволяют эффективно работать с диапазонами бит.

### 4.2 Ограничения C++ дизайна

1. **Нет итераторов/range-based for** — осознанное решение из-за proxy reference, но усложняет код. Kotlin может решить эту проблему через `Iterable<Int>` или `Sequence<Int>`, возвращающий индексы set-битов.

2. **Нет auto-grow** — `boost::dynamic_bitset` требует явного `resize` перед записью за пределы текущего размера. Java `BitSet` растёт автоматически. Для Kotlin нужно решить: явный resize vs auto-grow.

3. **Нет `nextClearBit`/`previousSetBit`** — `boost::dynamic_bitset` предоставляет только `find_first`/`find_next` для set-битов. Java BitSet предоставляет четыре навигационных метода.

4. **Нет `get(from, to)` для извлечения sub-bitset** — доступно в Java BitSet, отсутствует в обоих C++ реализациях.

5. **Нет `length()`** — логический размер (индекс MSB + 1) отсутствует в обоих C++ реализациях. Java BitSet различает `size()` (ёмкость) и `length()` (логический размер).

6. **Immutable вариант не рассмотрен** — обе реализации полностью мутабельные. C++ tradition leans mutable; Kotlin может предложить и immutable API.

7. **Нет `to_ullong` в boost** — несогласованность между std и boost API.

### 4.3 Уникальные идеи boost::dynamic_bitset

- **`empty()` vs `none()`** — явное разделение: `empty()` проверяет `size() == 0`, `none()` проверяет `count() == 0`. В Java BitSet `isEmpty()` эквивалентен `none()`.
- **`npos` sentinel** — аналог `std::string::npos` для `find_first`/`find_next`, вместо `-1` в Java.
- **Block-параметризация** — позволяет выбирать гранулярность хранилища (`uint8_t`, `uint16_t`, `uint32_t`, `uint64_t`). Для Kotlin stdlib такая параметризация, вероятно, избыточна.
- **`shrink_to_fit` + `reserve` + `capacity`** — знакомый паттерн из `std::vector`, позволяющий контролировать аллокации.
