# Шаг 4c. Сводный анализ использования BitSet в open-source репозиториях

## Резюме

На основе сконвергировавших данных из шага 4b (`422` use-site файла, `23` impl-файла, `23` репозитория) построены частотная таблица методов, оценка стабильности, классификация use-sites по 12 паттернам и каталог «неудобных» паттернов. Top-5 методов — `get(int)` (`253`), `set(int)` (`243`), `BitSet()` (`168`), `BitSet(int)` (`119`), `nextSetBit()` (`67`) — стабильны: top-5 order не меняется начиная с 2-го репозитория из 22 (incremental addition). Top-10 membership частично нестабильна: ранги 8–11 тесно сгруппированы (`clear(int)` `40`, `isEmpty()` `39`, `or()` `36`, `clear()` `33`), и исключение 3 из 22 крупных репозиториев меняет их состав.

Крупнейшие паттерны по method-bearing use-sites (`362` из `422`): Dataflow / liveness (`82`), Static analysis detectors (`64`), Query optimization (`48`), Indexing / record tracking (`44`), Column / parameter mask (`34`). По общему числу (включая `60` pass-through) Column / parameter mask поднимается на #2 (`71`), но `37` из них — pass-through (BitSet только как тип в сигнатуре). Главный «неудобный» паттерн — ручной цикл `for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))`, обнаруженный в `51` use-site файле из `67` с `nextSetBit()` (внутри канонического 4b-scope); exploratory grep по полным клонам (включая test/impl/gen) даёт `105` файлов. Четыре независимых immutability-обёртки (calcite, hibernate, graal FinalBitSet, druid) подтверждают потребность в read-only интерфейсе.

Сравнение с JetBrains-данными (step-03c) показывает устойчивый core: `get(int)`, `set(int)`, конструкторы и `nextSetBit()` доминируют в обеих выборках. Главное расхождение: `set(from,to)` занимает #2 в JetBrains (`63`) но #12 в OSS (`31`), что объясняется доминированием diff- и bytecode-кода в IntelliJ; наоборот, `cardinality()` и `size()` значительно выше в OSS.

## Входные данные

- [`step-04b-repo-data.md`](step-04b-repo-data.md) — per-repo каталоги BitSet-использования (канонический источник)
- [`step-04b-raw.tsv`](step-04b-raw.tsv) — машиночитаемые данные (`634` строки)
- [`step-03c-analysis.md`](step-03c-analysis.md) — анализ JetBrains-данных (для сравнения)

## 1. Scope & Methodology

### 1.1 Scope

- `23` репозитория, `641` файл после фильтрации false positives: `422` use, `23` impl, `178` test, `18` gen.
- `51` файл исключён как false positive (import без использования в коде).
- Среди `422` use-site файлов: `362` method-bearing, `60` pass-through (BitSet только как тип в сигнатуре).
- Общий scope `641` (включая `178` test и `18` gen) — из канонического [`step-04b-repo-data.md`](step-04b-repo-data.md). Машиночитаемый `step-04b-raw.tsv` покрывает `634` из `641` (не включает 5 use-sites и 2 test-файла, добавленных при ручной сверке 4b). Полный use-site subset (`422` файла) зафиксирован в [`step-04c-classified.tsv`](step-04c-classified.tsv).

### 1.2 Нормализация

Используется тот же словарь, что в `step-04b-repo-data.md` (раздел «Словарь нормализации»):

| Raw | Normalized |
|---|---|
| `new BitSet()` | `BitSet()` |
| `new BitSet(N)` / `BitSet(size)` | `BitSet(int)` |
| `BitSet.valueOf(...)` | `valueOf(...)` |
| `.set(int, boolean)` | `set(int,bool)` |
| `.isEmpty` (Kotlin property) | `isEmpty()` |
| `.size` (Kotlin property) | `size()` |

### 1.3 Верификация

- Частотная таблица перенесена из `step-04b-repo-data.md` (глобальная сводка), где арифметическая согласованность уже верифицирована: сумма per-repo use-site counts по каждому методу совпадает с глобальной таблицей.
- Классификация use-sites: сумма по категориям = `422`; per-repo суммы совпадают с verification table из `step-04b-repo-data.md`.
- Для вычислений стабильности использованы `422` use-файла из TSV (полное покрытие).

## 2. Частотная таблица

### 2.1 Глобальная таблица

| Ранг | Метод | Файлов | % от 422 |
|---:|---|---:|---:|
| 1 | `get(int)` | 253 | 59.9 |
| 2 | `set(int)` | 243 | 57.6 |
| 3 | `BitSet()` | 168 | 39.8 |
| 4 | `BitSet(int)` | 119 | 28.2 |
| 5 | `nextSetBit()` | 67 | 15.9 |
| 6 | `cardinality()` | 54 | 12.8 |
| 7 | `size()` | 48 | 11.4 |
| 8 | `clear(int)` | 40 | 9.5 |
| 9 | `isEmpty()` | 39 | 9.2 |
| 10 | `or()` | 36 | 8.5 |
| 11 | `clear()` | 33 | 7.8 |
| 12 | `set(from,to)` | 31 | 7.3 |
| 13 | `clone()` | 19 | 4.5 |
| 14 | `equals()` | 17 | 4.0 |
| 15 | `and()` | 15 | 3.6 |
| 15 | `length()` | 15 | 3.6 |
| 15 | `valueOf(...)` | 15 | 3.6 |
| 18 | `andNot()` | 14 | 3.3 |
| 19 | `nextClearBit()` | 13 | 3.1 |
| 20 | `set(int,bool)` | 12 | 2.8 |
| 20 | `toByteArray()` | 12 | 2.8 |
| 22 | `stream()` | 7 | 1.7 |
| 23 | `set(from,to,bool)` | 6 | 1.4 |
| 24 | `clear(from,to)` | 5 | 1.2 |
| 24 | `previousSetBit()` | 5 | 1.2 |
| 24 | `toString()` | 5 | 1.2 |
| 27 | `get(from,to)` | 3 | 0.7 |
| 27 | `toLongArray()` | 3 | 0.7 |
| 29 | `hashCode()` | 2 | 0.5 |
| 29 | `intersects()` | 2 | 0.5 |
| 31 | `flip(int)` | 1 | 0.2 |
| 31 | `flip(from,to)` | 1 | 0.2 |
| 31 | `previousClearBit()` | 1 | 0.2 |

### 2.2 Группировка по семействам операций

| Семейство | Методы | Σ method-file freq | Доля |
|---|---|---:|---:|
| **Construction** | `BitSet()`, `BitSet(int)` | 287 | — |
| **Single-bit read/write** | `get(int)`, `set(int)`, `set(int,bool)`, `clear(int)`, `flip(int)` | 549 | — |
| **Range operations** | `get(from,to)`, `set(from,to)`, `set(from,to,bool)`, `clear(from,to)`, `flip(from,to)` | 46 | — |
| **Bulk bitwise** | `and()`, `or()`, `andNot()`, `xor()` (xor: 0 в данных) | 65 | — |
| **Navigation** | `nextSetBit()`, `nextClearBit()`, `previousSetBit()`, `previousClearBit()` | 86 | — |
| **Query** | `isEmpty()`, `cardinality()`, `size()`, `length()`, `intersects()` | 158 | — |
| **Conversion / serialization** | `toByteArray()`, `toLongArray()`, `valueOf(...)`, `stream()` | 37 | — |
| **Identity / copy** | `clone()`, `equals()`, `hashCode()`, `toString()` | 43 | — |

**Примечание.** Числа в столбце «Σ method-file freq» — суммы per-method file frequencies из §2.1, а не union-counts уникальных файлов. Один файл, использующий несколько методов одного семейства, учтён по разу на каждый метод (например, Construction: `168 + 119 = 287`, тогда как union-count = `279` при `8` overlapping файлах — см. §9.1). Значения также не суммируются вертикально, т.к. один файл может использовать методы из нескольких семейств.

### 2.3 Наблюдения

- OSS-профиль доминируется single-bit access (`get(int)` + `set(int)` — в `60%` файлов каждый) и конструкторами (`BitSet()` `40%`, `BitSet(int)` `28%`).
- `nextSetBit()` (#5, `67` файлов) — единственный navigation-метод в top-10; это сигнал к обязательному inclusion в stdlib API и потребности в Kotlin-friendly обёртке (итератор / sequence).
- `cardinality()` (#6, `54`) и `size()` (#7, `48`) высоко в рейтинге, но семантически неоднородны: `size()` в `java.util.BitSet` возвращает ёмкость хранилища в битах, а не число set-битов; при проектировании stdlib API необходимо чётко разделить `count` / `cardinality` vs `capacity`.
- Range operations (`set(from,to)` = `31`, `7.3%`) значительно менее частотны, чем в JetBrains-выборке (`63`, #2), но всё ещё используются в `31` файле. Они остаются необходимыми.
- `xor()` не встретился ни разу (в JetBrains = `1`); `flip()` — по `1` файлу. Оба — low-priority для core API.

## 3. Оценка стабильности

### 3.1 Leave-one-out analysis

Для каждого из `22` репозиториев с use-site файлами в TSV (RoaringBitmap: 0 use-файлов) — убираем один репозиторий, пересчитываем глобальные частоты, проверяем два триггера нестабильности.

| Repo | use | Top-10 membership Δ | Top-5 order Δ |
|---|---:|---|---|
| graal | 94 | −`or()`; +`clear()` | — |
| spotbugs | 65 | −`or()`; +`set(from,to)` | `set(int)` > `get(int)` > `BitSet()` > `BitSet(int)` > `nextSetBit()` |
| elasticsearch | 45 | — | — |
| hive | 36 | — | — |
| hibernate-orm | 35 | — | — |
| checkstyle | 24 | — | `set(int)` > `get(int)` > `BitSet()` > `BitSet(int)` > `nextSetBit()` |
| flink | 19 | — | — |
| calcite | 16 | −`or()`; +`clear()` | — |
| h2database | 16 | — | — |
| druid | 13 | — | — |
| lucene | 13 | — | — |
| antlr4 | 12 | — | — |
| guava | 6 | — | — |
| cassandra | 5 | — | — |
| androidx | 4 | — | — |
| beam | 4 | — | — |
| eclipse-collections | 4 | — | — |
| netty | 4 | — | — |
| pmd | 3 | — | — |
| spring-framework | 2 | — | — |
| commons-lang | 1 | — | — |
| spark | 1 | — | — |

**Top-10 membership:** `3` из `22` репозиториев вызывают изменения при исключении (graal, spotbugs, calcite). Причина: ранги 8–11 (`clear(int)` `40`, `isEmpty()` `39`, `or()` `36`, `clear()` `33`) тесно сгруппированы — разброс `7` файлов. Исключение тяжёлого `or()`-пользователя (graal, spotbugs, calcite) сдвигает `or()` ниже порога top-10.

**Top-5 order:** `2` из `22` — spotbugs и checkstyle. При их исключении `set(int)` обгоняет `get(int)` (gap `4`–`5` файлов). Это объясняется близостью значений: оба метода используются в ~`60%` файлов, и удаление крупного `get`-heavy репозитория (static analysis) меняет порядок.

### 3.2 Incremental convergence

Репозитории добавляются по убыванию числа use-site файлов (tie-break: алфавитный порядок имени репозитория, ascending). Tie-break для методов на границе top-10 при совпадении счётчиков: алфавитный порядок имени метода (ascending).

| # | Repo | use | Cumul | Top-5 order | Top-10 Δ vs prev |
|---:|---|---:|---:|---|---|
| 1 | graal | 94 | 94 | `set(int)`, `get(int)`, `BitSet(int)`, `BitSet()`, `nextSetBit()` | (initial) |
| 2 | spotbugs | 65 | 159 | `get(int)`, `set(int)`, `BitSet()`, `BitSet(int)`, `nextSetBit()` | +`clear()`; −`andNot()` |
| 3 | elasticsearch | 45 | 204 | — | +`cardinality()`; −`clone()` ¹ |
| 4 | hive | 36 | 240 | — | +`size()`; −`isEmpty()` ² |
| 5–8 | hibernate-orm…calcite | 35…16 | 334 | — | — |
| 9 | h2database | 16 | 350 | — | +`isEmpty()`; −`clear()` |
| 10–22 | druid…spark | 13…1 | 422 | — | — |

**Точки стабилизации:**
- **Top-5 order:** стабилен с шага 2 (после spotbugs, `159` файлов). Порядок: `get(int)` > `set(int)` > `BitSet()` > `BitSet(int)` > `nextSetBit()`.
- **Top-10 membership:** стабилен с шага 9 (после h2database, `350` файлов). На этом шаге `isEmpty()` вошёл в top-10, вытеснив `clear()`, после чего состав не менялся.
- **Full stabilization:** шаг 9.

¹ Boundary tie: `cardinality()` = `set(from,to)` = `16`; alphabetical tie-break ставит `cardinality()` в top-10.
² Boundary tie: `cardinality()` = `isEmpty()` = `23`; `cardinality()` уже в top-10 с шага 3, `isEmpty()` остаётся за пределами.

### 3.3 Вывод

| Критерий | Результат |
|---|---|
| LOO: top-10 membership stable | Нет (3 из 22 repo trigger) |
| LOO: top-5 order stable | Нет (2 из 22 repo trigger) |
| Incremental: top-5 stabilization | Шаг 2 (159 файлов) |
| Incremental: top-10 stabilization | Шаг 9 (350 файлов) |
| Incremental: full stabilization | Шаг 9 (350 файлов) |

**Интерпретация.** Полная стабилизация (по формальному критерию) не достигнута в LOO-анализе: `3` из `22` репозиториев вызывают top-10 churn. Однако нестабильность ограничена рангами 8–11, где разброс = `7` файлов. Top-5 лидеры (`get(int)`, `set(int)`, `BitSet()`, `BitSet(int)`, `nextSetBit()`) устойчивы: порядок фиксируется уже после двух крупнейших репозиториев и не меняется при LOO для 20 из 22 repos. Частичная стабилизация (по аналогии с step-03c) достигнута: лидеры зафиксированы, bottom-4 top-10 — зона неопределённости.

## 4. Классификация use-sites

### 4.1 Методология

Каждый из `422` use-site файлов отнесён ровно к одной категории. Классификация выполнена repo-by-repo с использованием трёх сигналов:

1. **Домен репозитория** (сильнейший сигнал): compilers → Dataflow; static analysis → Static analysis detectors; databases → Query optimization, etc.
2. **Путь файла**: ключевые слова (`/liveness/`, `/detect/`, `/checks/`, `/calcite/`, `/sql/results/`) для внутрикатегориального разбиения.
3. **Контекст** (1 строка из MD/TSV) и **профиль методов** (secondary signal).

Категории адаптированы из `10` категорий step-03c: `3` новых добавлены (Static analysis detectors, Null/dirty tracking, Query optimization); `Diff / text comparison` (специфична для IntelliJ) исключена (0 файлов в OSS-выборке); `Bytecode offset tracking` обобщена до `Bytecode / IR tracking`.

Pass-through файлы (BitSet только как тип в сигнатуре, `60` штук) классифицированы по модулю/пакету, в котором они находятся. Распределение pass-through неравномерно: `37` из `60` попали в Column / parameter mask (hibernate-orm, elasticsearch), что поднимает её с #5 (method-bearing `34`) на #2 (all `71`). Столбец M-B в таблице ниже показывает method-bearing count для каждой категории.

### 4.2 Категории

| # | Категория | Count | M-B | % | Critical methods (top-5) |
|---:|---|---:|---:|---:|---|
| 1 | Dataflow / liveness | 87 | 82 | 20.6 | `get(int)` (59), `set(int)` (53), `BitSet()` (39), `BitSet(int)` (29), `nextSetBit()` (20) |
| 2 | Column / parameter mask | 71 | 34 | 16.8 | `set(int)` (21), `get(int)` (18), `BitSet(int)` (14), `BitSet()` (7), `nextSetBit()` (6) |
| 3 | Static analysis detectors | 67 | 64 | 15.9 | `get(int)` (54), `BitSet()` (31), `set(int)` (30), `nextSetBit()` (7), `clear()` (6) |
| 4 | Query optimization | 48 | 48 | 11.4 | `set(int)` (34), `get(int)` (32), `BitSet()` (30), `BitSet(int)` (12), `cardinality()` (11) |
| 5 | Indexing / record tracking | 46 | 44 | 10.9 | `set(int)` (36), `get(int)` (34), `BitSet()` (21), `BitSet(int)` (21), `size()` (8) |
| 6 | Flag / feature storage | 30 | 29 | 7.1 | `set(int)` (24), `get(int)` (17), `BitSet()` (14), `BitSet(int)` (13), `cardinality()` (8) |
| 7 | Set membership / visited | 20 | 16 | 4.7 | `get(int)` (11), `set(int)` (10), `BitSet(int)` (8), `BitSet()` (7), `isEmpty()` (7) |
| 8 | Bytecode / IR tracking | 15 | 12 | 3.6 | `set(int)` (11), `get(int)` (10), `BitSet(int)` (6), `BitSet()` (6), `length()` (4) |
| 9 | Serialization / wire protocol | 12 | 9 | 2.8 | `set(int)` (5), `BitSet()` (5), `get(int)` (4), `valueOf(...)` (3), `BitSet(int)` (2) |
| 10 | Character class / lexer | 10 | 10 | 2.4 | `set(int)` (9), `get(int)` (9), `nextSetBit()` (5), `BitSet()` (4), `cardinality()` (4) |
| 11 | Null / dirty tracking | 10 | 9 | 2.4 | `set(int)` (8), `BitSet(int)` (6), `size()` (4), `get(int)` (3), `BitSet()` (3) |
| 12 | Graph / type-state | 6 | 5 | 1.4 | `and()` (3), `or()` (3), `andNot()` (3), `cardinality()` (3), `get(int)` (2) |
| — | **Итого** | **422** | **362** | **100.0** | — |

### 4.3 Per-repo breakdown

Аббревиатуры: DF = Dataflow/liveness, CM = Column/parameter mask, SD = Static analysis detectors, QO = Query optimization, IR = Indexing/record tracking, FS = Flag/feature storage, SM = Set membership/visited, BT = Bytecode/IR tracking, SP = Serialization, CL = Character class/lexer, ND = Null/dirty tracking, GT = Graph/type-state.

| Repo | DF | CM | SD | QO | IR | FS | SM | BT | SP | CL | ND | GT | Total |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| graal | 55 | 13 |  |  |  | 5 | 2 | 8 | 5 |  |  | 6 | 94 |
| spotbugs | 24 |  | 40 |  |  |  |  | 1 |  |  |  |  | 65 |
| elasticsearch |  | 19 |  |  | 8 | 1 |  | 6 |  | 2 | 9 |  | 45 |
| hive |  |  |  | 26 |  | 6 |  |  | 3 |  | 1 |  | 36 |
| hibernate-orm |  | 30 |  |  |  | 4 | 1 |  |  |  |  |  | 35 |
| checkstyle |  |  | 24 |  |  |  |  |  |  |  |  |  | 24 |
| flink |  | 6 |  | 3 |  | 9 |  |  | 1 |  |  |  | 19 |
| calcite |  |  |  | 16 |  |  |  |  |  |  |  |  | 16 |
| h2database |  | 3 |  | 1 | 12 |  |  |  |  |  |  |  | 16 |
| lucene |  |  |  |  | 10 |  |  |  |  | 3 |  |  | 13 |
| druid |  |  |  | 2 | 11 |  |  |  |  |  |  |  | 13 |
| antlr4 | 8 |  |  |  |  |  | 4 |  |  |  |  |  | 12 |
| guava |  |  |  |  |  |  | 4 |  |  | 2 |  |  | 6 |
| cassandra |  |  |  |  | 4 |  |  |  | 1 |  |  |  | 5 |
| beam |  |  |  |  |  | 1 | 1 |  | 2 |  |  |  | 4 |
| androidx |  |  |  |  |  | 1 | 3 |  |  |  |  |  | 4 |
| netty |  |  |  |  |  | 1 |  |  |  | 3 |  |  | 4 |
| eclipse-collections |  |  |  |  |  |  | 4 |  |  |  |  |  | 4 |
| pmd |  |  | 3 |  |  |  |  |  |  |  |  |  | 3 |
| spring-framework |  |  |  |  |  | 2 |  |  |  |  |  |  | 2 |
| spark |  |  |  |  | 1 |  |  |  |  |  |  |  | 1 |
| commons-lang |  |  |  |  |  |  | 1 |  |  |  |  |  | 1 |
| **Итого** | **87** | **71** | **67** | **48** | **46** | **30** | **20** | **15** | **12** | **10** | **10** | **6** | **422** |

### 4.4 Наблюдения

- **Single-repo concentration:** 5 крупнейших репозиториев (graal `94`, spotbugs `65`, elasticsearch `45`, hive `36`, hibernate `35`) дают `275` / `422` = `65%` всех use-sites. Это создаёт domain bias: compiler-related паттерны (Dataflow + Bytecode = `102`) доминируют за счёт graal.
- **Кластер «множество элементов»** (Dataflow, Static analysis, Set membership, Graph): `180` use-sites (`43%`). Характерные методы: `get(int)`, `set(int)`, `or()`, `nextSetBit()`. Эти use cases нуждаются в эффективных точечных обновлениях, merge-операциях и итерации по set-битам.
- **Кластер «табличная проекция»** (Column mask, Query optimization, Null tracking, Indexing): `175` use-sites (`41%`). Характерные методы: `set(int)`, `get(int)`, `BitSet(int)`, `size()`, `cardinality()`. Этим use cases важны конструктор с размером, подсчёт set-битов и ясная семантика `size` vs `cardinality`.
- **Graph / type-state** (`6` use-sites) — единственная категория, где bulk bitwise (`and`, `or`, `andNot`) доминируют над access primitives. Это подтверждает, что bulk algebra — specialized need для алгоритмических use cases, а не основной паттерн массового использования.

## 5. Каталог «неудобных» паттернов

### 5.1 Ручные циклы по nextSetBit (отсутствие итератора)

**Масштаб:** `51` use-site файл (из `67` с `nextSetBit()` в каноническом 4b-scope из `422`) содержит паттерн `for(...nextSetBit...)` или `while(...nextSetBit...)`. Exploratory grep по полным клонам (включая test, impl, gen) даёт `105` файлов.

Каноническая форма:
```java
for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
    // operate on index i here
    if (i == Integer.MAX_VALUE) {
        break; // or (i+1) would overflow
    }
}
```

**Примеры:**

1. **checkstyle** — `IndentLevel.java:51`:
```java
for (int i = src.nextSetBit(0); i >= 0; i = src.nextSetBit(i + 1)) {
    addAcceptedIndent(new Indent(i + offset, option));
}
```

2. **spotbugs** — `MergeTree.java:120`:
```java
for (int i = inputSet.nextSetBit(0); i >= 0; i = inputSet.nextSetBit(i + 1)) {
    inputFactList.add(valueNumberDataflow.getFactAtLocation(mergeInputValues[i]));
}
```

3. **calcite** — `RelMdPredicates.java:1041`:
```java
for (int j = 0, i = fields.nextSetBit(0); i >= 0; i = fields.nextSetBit(i + 1), j++) {
    argsPulledUp.add(rexBuilder.makeInputRef(joinRel, i));
}
```

4. **antlr4** — `GrammarParserInterpreter.java:291` (while-loop variant):
```java
int alt = alts.nextSetBit(0);
while ( alt>=0 ) {
    parser.reset();
    parser.addDecisionOverride(decision, startIndex, alt);
    ParserRuleContext t = parser.parse(startRuleIndex);
    trees.add(ambigSubTree);
    alt = alts.nextSetBit(alt+1);
}
```

**Проблема:** 7-строчный бойлерплейт с неочевидным `Integer.MAX_VALUE` guard. Встречается в двух формах (`for` и `while`), обе требуют от разработчика помнить sentinel `>= 0` и инкремент `+ 1`. Kotlin stdlib BitSet должен предоставить `forEachSetBit {}`, `iterator()` или `asSequence()`.

### 5.2 clone() as BitSet — unsafe cast

**Масштаб:** `19` use-файлов вызывают `clone()`. В Java `BitSet.clone()` возвращает `Object`, требуя unsafe cast.

**Пример — graal** `BytecodeParser.java`:
```java
BitSet newbs = (BitSet) bitset.clone();
```

**Пример — calcite** `BitSets.java:249` — utility для union:
```java
public static BitSet union(BitSet set0, BitSet... sets) {
    final BitSet s = (BitSet) set0.clone();
    for (BitSet set : sets) {
        s.or(set);
    }
    return s;
}
```
Calcite пришлось создать целый utility-класс `BitSets` со static helpers, каждый из которых начинается с `(BitSet) x.clone()`.

**Пример — spotbugs** `FindNullDeref.java:643`:
```java
nullArgSet = (BitSet) nullArgSet.clone();
definitelyNullArgSet = (BitSet) definitelyNullArgSet.clone();
```

**Проблема:** В Kotlin нет аналога `clone()` с правильным типом возврата. Stdlib BitSet должен предоставить typed `copy()`.

### 5.3 stream() для итерации (Java 8+ workaround)

**Масштаб:** `7` use-файлов вызывают `stream()`.

**Пример — graal** `SubstrateReferenceMap.java`:
```java
offsets.stream().forEach(offset -> { ... });
```

**Пример — calcite** `HyperGraph.java:327` — `stream().toArray()` для enhanced-for:
```java
for (int index : unionOverlapBitSet.stream().toArray()) {
    HyperEdge edge = edges.get(index);
    if (isAccurateEdge(edge, unionSet)) {
        unionComplexBitSet.set(index);
    }
}
```

**Пример — calcite** `HyperGraph.java:205` — `stream().mapToObj().collect()`:
```java
List<HyperEdge> simpleEdges = simpleEdgesMap.getOrDefault(csg, new BitSet()).stream()
    .mapToObj(edges::get)
    .collect(Collectors.toList());
```

**Проблема:** `stream()` — Java-specific (`IntStream`, не `Stream<Integer>`); недоступно на non-JVM platforms. Даже на JVM требует `.mapToObj()` для конвертации индексов в объекты или `.toArray()` для materialization в `int[]`. Kotlin stdlib BitSet должен предоставить Kotlin-native iteration API.

### 5.4 Обёртки для иммутабельности

**Масштаб:** `4` независимых реализации read-only / immutable BitSet в `4` репозиториях.

| Repo | Файл | Подход |
|---|---|---|
| calcite | `ImmutableBitSet.java` | Полноценный immutable тип с defensive copy + set operations возвращают новый объект |
| hibernate | `ImmutableBitSet.java` | Delegation wrapper + contains (subset check) |
| graal | `FinalBitSet.java` | `@CompilationFinal long[]` для JIT constant-folding; proxy всех get-методов |
| druid | `WrappedImmutableBitSetBitmap.java` | Adapter: immutable BitSet к bitmap index API |

**Отдельно:** graal `MultiTypeStateWithBitSet.java` — domain-specific контейнер (BitSet + cached cardinality + type-state metadata); основной мотив — кэширование `cardinality()` (j.u.BitSet пересчитывает его при каждом вызове), а не immutable surface. Описан в разделе 7.6.

**Пример — calcite** `ImmutableBitSet.java` (1100+ строк, полная реимплементация):
```java
/**
 * An immutable list of bits.
 */
public class ImmutableBitSet
    implements Iterable<Integer>, Serializable, Comparable<ImmutableBitSet> {
  private static final int ADDRESS_BITS_PER_WORD = 6;
  private static final long WORD_MASK = 0xffffffffffffffffL;
  private static final long[] EMPTY_LONGS = new long[0];
  // ... 1100+ строк собственного long[]-based storage и bit manipulation
}
```

**Пример — hibernate** `ImmutableBitSet.java`:
```java
/**
 * An immutable variant of the {@link BitSet} class with some additional operations.
 */
public final class ImmutableBitSet {
    private final BitSet bitSet;
    ...
}
```

**Проблема:** Четыре независимых обёртки — сильный сигнал к тому, что core API должен либо предоставить immutable variant, либо read-only view (по аналогии с `List`/`MutableList` в Kotlin collections).

### 5.5 Serialization wrappers

**Масштаб:** `3` impl-файла в `2` репозиториях, посвящённые сериализации BitSet.

| Repo | Файл | Описание |
|---|---|---|
| beam | `BitSetCoder.java` (sdk/coders) | Coder для Apache Beam pipeline; кодирует через `toByteArray()` / `valueOf(byte[])` |
| beam | `BitSetCoder.java` (sdk/util) | Аналогичный coder в legacy-пакете |
| graal | `RecordedOperationPersistence.java` | JSON-сериализация BitSet как `long[]` для replay compilation |

**Пример — beam** `BitSetCoder.java`:
```java
@Override
public void encode(BitSet value, OutputStream outStream) throws IOException {
    byte[] bytes = value.toByteArray();
    VarInt.encode(bytes.length, outStream);
    outStream.write(bytes);
}
```

**Проблема:** `toByteArray()` / `toLongArray()` + `valueOf()` — стандартный round-trip, но нет встроенной поддержки форматов сериализации. Для stdlib BitSet минимально нужны byte/long-array interop: `toByteArray()` / `fromByteArray()` и `toLongArray()` / `fromLongArray()` (или эквиваленты). Среди всех `15` valueOf-файлов в use-scope 4c overload `byte[]` доминирует: `10` файлов (cassandra, elasticsearch-sql ×2, h2database, hive ×4, lucene, androidx/ProfileTranscoder) против `5` файлов с `long[]` (graal ×2, elasticsearch-esql, hive/GroupingSetOptimizer, androidx/SystemPropertyResolver).

### 5.6 Compressed bitmap adapters

**Масштаб:** `6` impl-файлов в `2` репозиториях.

| Repo | Файл | Описание |
|---|---|---|
| RoaringBitmap | `BitSetUtil.java` | Конвертация j.u.BitSet ↔ RoaringBitmap; прямой доступ к `toLongArray()` |
| RoaringBitmap | `RoaringBitSet.java` | Drop-in replacement: `extends BitSet`, делегирует на RoaringBitmap |
| RoaringBitmap | `BufferBitSetUtil.java` | Аналогичная конвертация для buffer-backed variant |
| druid | `BitSetBitmapFactory.java` | Factory для j.u.BitSet в контексте bitmap index API |
| druid | `WrappedBitSetBitmap.java` | Mutable adapter j.u.BitSet → `ImmutableBitmapMethods` |
| druid | `WrappedImmutableBitSetBitmap.java` | Immutable adapter (см. 5.4) |

**Проблема:** Не gap core BitSet API — это domain-specific data structure adapters. Однако byte/long-array interop (`toByteArray()` / `fromByteArray()`, `toLongArray()` / `fromLongArray()`) критичен для интеграции с compressed bitmap libraries.

### 5.7 Fluent / convenience wrappers

**Масштаб:** `3` impl-файла.

| Repo | Файл | Описание |
|---|---|---|
| commons-lang | `FluentBitSet.java` | Fluent delegation wrapper: `set(int).set(int).and(other)` chain |
| spotbugs | `MethodBytecodeSet.java` | `extends BitSet` + пользовательский `toString()` (bit positions → JVM opcode names) |
| netty | `HttpChunkLineValidatingByteProcessor.java` | Custom char-class matching via bit-level operations |

**Пример — commons-lang** `FluentBitSet.java`:
```java
/**
 * A convenient way of working with {@link BitSet} by providing a fluent interface
 * to java.util.BitSet.
 */
public final class FluentBitSet implements Cloneable, Serializable {
    private final BitSet bitSet;
    public FluentBitSet set(int bitIndex) {
        bitSet.set(bitIndex);
        return this;
    }
    ...
}
```

**Проблема:** `FluentBitSet` — ~600 строк чистого boilerplate delegation, добавляющего `return this` к каждому мутирующему методу. Сигнал о том, что `java.util.BitSet` API неудобен: `and()`, `or()` возвращают `void`, делая chaining невозможным. Kotlin stdlib может решить это через operator overloading и extension functions вместо fluent wrappers.

### 5.8 SparseBooleanArray как альтернатива (Android)

В `androidx` обнаружено `14` файлов с `SparseBooleanArray` (`9` без тестов), но ни один не использует его совместно с `java.util.BitSet` в одном файле. `SparseBooleanArray` — Android-specific sparse structure для небольших множеств, не замена BitSet. В контексте stdlib BitSet это подтверждает: Android-экосистема использует BitSet для dense множеств и SparseBooleanArray для sparse; stdlib BitSet должен оставаться dense.

## 6. Сравнение с JetBrains-данными (step-03c)

### 6.1 Top-10

| Ранг | JetBrains Combined (180 loci) | Count | OSS (422 файла) | Count |
|---:|---|---:|---|---:|
| 1 | `get(int)` | 75 | `get(int)` | 253 |
| 2 | `set(from,to)` | 63 | `set(int)` | 243 |
| 3 | `BitSet()` | 60 | `BitSet()` | 168 |
| 4 | `set(int)` | 52 | `BitSet(int)` | 119 |
| 5 | `BitSet(int)` | 32 | `nextSetBit()` | 67 |
| 6 | `set(int,bool)` | 32 | `cardinality()` | 54 |
| 7 | `nextSetBit()` | 29 | `size()` | 48 |
| 8 | `or()` | 21 | `clear(int)` | 40 |
| 9 | `isEmpty()` | 18 | `isEmpty()` | 39 |
| 10 | `cardinality()` | 15 | `or()` | 36 |

### 6.2 Ключевые расхождения

| Метод | JetBrains | OSS | Δ | Причина |
|---|---:|---:|---|---|
| `set(from,to)` | #2 (63) | #12 (31) | −10 рангов | IntelliJ diff/bytecode layer генерирует range-heavy use-sites; в OSS range ops менее частотны |
| `set(int,bool)` | #6 (32) | #20 (12) | −14 рангов | IntelliJ graph/flag modules используют `set(i, true/false)` чаще, чем OSS repos |
| `cardinality()` | #10 (15) | #6 (54) | +4 ранга | OSS repos (spotbugs, flink, hive) активно считают set-биты |
| `size()` | (8) | #7 (48) | — | OSS repos часто вызывают `size()` (capacity); JetBrains — реже, чаще через wrappers |
| `clear(int)` | (9) | #8 (40) | — | В JetBrains не входил в top-10; в OSS — входит |

### 6.3 Совпадения

- **Общий core** в обеих выборках включает `get(int)`, `BitSet()`, `set(int)` и `nextSetBit()`, но JetBrains top-3 смещён в сторону `set(from,to)` (#2), а `set(int)` опускается на #4.
- **`nextSetBit()`** — must-have в обеих: #5 в OSS, #7 в JetBrains.
- **`or()`** и **`isEmpty()`** — в top-10 обеих выборок.
- **`clone()` / `copy()`** — не в top-10 ни одной выборки, но устойчиво востребован: `19` файлов в OSS, `7` в JetBrains; при этом в JetBrains `copy()` is a known repeated gap (BitSetUtil, symbolLightUtils, LivenessAnalysis).

### 6.4 Сравнение паттернов

| Категория (step-03c) | JetBrains | OSS-аналог | OSS |
|---|---:|---|---:|
| Dataflow / liveness | 35 | Dataflow / liveness | 87 |
| Diff / text comparison | 27 | — (0 в OSS) | 0 |
| Bytecode offset tracking | 26 | Bytecode / IR tracking | 15 |
| Indexing / file ID containers | 21 | Indexing / record tracking | 46 |
| Graph algorithms | 18 | Graph / type-state | 6 |
| Serialization protocol | 18 | Serialization / wire protocol | 12 |
| Flag storage | 16 | Flag / feature storage | 30 |
| Set membership / visited | 8 | Set membership / visited | 20 |
| Parameter mask / overload | 8 | Column / parameter mask | 71 |
| Character class / lexer | 3 | Character class / lexer | 10 |
| — | — | Static analysis detectors (NEW) | 67 |
| — | — | Query optimization (NEW) | 48 |
| — | — | Null / dirty tracking (NEW) | 10 |

**Ключевые наблюдения:**
- `Diff / text comparison` (`27` в JetBrains) — полностью отсутствует в OSS; это JetBrains-specific категория.
- `Static analysis detectors` (`67` в OSS) — новая крупная категория, отсутствующая в JetBrains-выборке (IntelliJ — IDE, не static analyzer).
- `Column / parameter mask` взрывается с `8` до `71` при переходе к OSS: hibernate, elasticsearch, flink — все используют BitSet для column projection.
- `Query optimization` (`48`) — новая категория; calcite + hive + flink-calcite rules.

### 6.5 Combined must-have signal

1. **API core — intersection обоих top-10**: `get(int)`, `set(int)`, `BitSet()`, `BitSet(int)`, `nextSetBit()`, `or()`, `isEmpty()`, `cardinality()` (8 методов).
2. **One-sided, но значимые**: `set(int,bool)` (JetBrains #6, OSS #20), `clear(int)` (OSS #8, вне JetBrains top-10), `size()` (OSS #7, вне JetBrains top-10).
3. **Iteration need**: ручной цикл `nextSetBit` — доминирующий паттерн в обеих выборках. Kotlin-friendly итерация — обязательна.
4. **Typed copy**: `clone()` (`19` OSS) / `copy()` (`7` JetBrains) — повторяющийся gap.
5. **Immutability wrappers**: `4` в OSS + `1` в JetBrains (syntax MutableBitSet/BitSet). Ещё `3` JetBrains-артефакта (com.intellij.util.diff.BitSet, fleet.util.BitSet, text-matching typealias) — multiplatform reimplementations, а не immutable variants.
6. **Range operations**: менее частотны в OSS (`31` vs `63`), но по-прежнему нужны.

## 7. Каталог impl-файлов

`23` impl-файла из step-04b, организованные по восполняемому gap. Bucket-ы тематические и допускают overlap: calcite `ImmutableBitSet.java` входит и в §7.1 (Immutability), и в §7.2 (Iteration); поэтому entry counts по bucket-ам суммируются в `24`, а не `23`.

### 7.1 Immutability (4 файла)

| Repo | Файл | Подход |
|---|---|---|
| calcite | `ImmutableBitSet.java` | Полноценный immutable тип, `Iterable<Integer>`, `Comparable` |
| hibernate | `ImmutableBitSet.java` | Delegation wrapper с defensive copy |
| graal | `FinalBitSet.java` | `@CompilationFinal long[]` для JIT; read-only proxy |
| druid | `WrappedImmutableBitSetBitmap.java` | Immutable adapter к bitmap API |

### 7.2 Iteration / Iterable (3 файла)

| Repo | Файл | Подход |
|---|---|---|
| calcite | `ImmutableBitSet.java` | `implements Iterable<Integer>` |
| calcite | `BitSets.java` | Utility: `toIter()`, `toList()`, `union()`, `contains()` |
| graal | `Util.java` (espresso) | `mergeBitSets` + `Iterable<Integer>` iterators |

### 7.3 Serialization (3 файла)

| Repo | Файл | Подход |
|---|---|---|
| beam | `BitSetCoder.java` (coders) | `toByteArray()` / `valueOf(byte[])` round-trip |
| beam | `BitSetCoder.java` (util) | Legacy duplicate |
| graal | `RecordedOperationPersistence.java` | JSON serialization as `long[]` |

### 7.4 Compressed storage (3 файла)

| Repo | Файл | Подход |
|---|---|---|
| RoaringBitmap | `BitSetUtil.java` | j.u.BitSet ↔ RoaringBitmap conversion |
| RoaringBitmap | `RoaringBitSet.java` | Drop-in `extends BitSet` |
| RoaringBitmap | `BufferBitSetUtil.java` | Buffer-backed conversion |

### 7.5 Fluent / convenience (3 файла)

| Repo | Файл | Подход |
|---|---|---|
| commons-lang | `FluentBitSet.java` | Fluent method chaining wrapper |
| spotbugs | `MethodBytecodeSet.java` | Custom `toString()` (bit positions → JVM opcode names) |
| netty | `HttpChunkLineValidatingByteProcessor.java` | Char-class matching via bit ops |

### 7.6 Domain-specific adapters (8 файлов)

| Repo | Файл | Подход |
|---|---|---|
| graal | `MultiTypeStateWithBitSet.java` | BitSet + cached cardinality + type-state metadata |
| druid | `BitSetBitmapFactory.java` | Factory для bitmap index API |
| druid | `WrappedBitSetBitmap.java` | Mutable adapter к bitmap API |
| guava | `CharMatcher.java` (×2) | `BitSetMatcher` for fast char classification |
| h2database | `FreeSpaceBitSet.java` | Free-space tracking in MVStore |
| beam | `FinishedTriggersBitSet.java` | Delegation wrapper: trigger-state tracking с `copy()` через `clone()`, range-clear |
| spotbugs | `BlockType.java` | `extends BitSet` dataflow-lattice: validity, top/bottom, depth; `copyFrom()` через `clear()`+`or()` |

## 8. Ограничения

1. **Domain bias.** 5 крупнейших репозиториев (`65%` use-sites) смещают выборку в сторону compiler/static-analysis и database use cases. Web frameworks, mobile и networking представлены минимально.
2. **Java-only.** Kotlin OSS repos практически не представлены (только `androidx` с `4` use-файлами). Выборка отражает Java-экосистему, а не Kotlin-first usage.
3. **Regex-based extraction.** Disambiguation `set(int,bool)` vs `set(from,to)` — эвристика; возможно завышение `set(from,to)` на ≤5 файлов. Ambiguous `get(int)` / `size()` — возможно завышение на 5–10%.
4. **File-level granularity.** Частоты считают число файлов, а не число call-sites. Файл с 1 вызовом `get(int)` и файл с 50 вызовами имеют одинаковый вес.
5. **Heuristic classification.** Категоризация use-sites выполнена на основе пути файла, контекста и домена репозитория; ручная проверка каждого из 422 файлов не проводилась. Confidence: high `246`, medium `93`, low `83` файлов.
6. **Guava double-counting.** Guava содержит зеркальные деревья `android/guava/` и `guava/`; обе версии каталогизированы, что даёт +3 дублированных use-site файла (`+1` Character class / lexer: SmallCharMatcher, `+2` Set membership / visited: ImmutableMap, Sets), завышая частоты в обеих категориях.

## 9. Implications for API Design

### 9.1 Подтверждённый must-have core

OSS-данные подтверждают и усиливают выводы step-03c:

| Surface | Основание |
|---|---|
| `BitSet()`, `BitSet(capacity)` | `279` файлов (66% union-count; `168` + `119` individually, `8` overlap) |
| `get(int)`, `set(int)`, `clear(int)`, `set(int, value)` | `253` / `243` / `40` / `12` файлов |
| `nextSetBit()` + Kotlin-friendly iteration | `67` файлов; `51` use-site файл с ручным nextSetBit-циклом (`105` across full clones) |
| `or()`, `and()`, `andNot()` | `36` / `15` / `14` файлов |
| `isEmpty()`, `cardinality()` | `39` / `54` файлов |
| Typed `copy()` | `19` clone() + JetBrains `7` copy(); `5` immutability wrappers |
| `equals()`, `hashCode()` | `17` / `2` файлов |

### 9.2 Подтверждённые nice-to-have

| Surface | Основание |
|---|---|
| `set(from,to)` / `clear(from,to)` | `31` / `5` файлов (меньше чем в JetBrains, но всё ещё значимо) |
| `nextClearBit()` | `13` файлов |
| byte/long-array interop (`toByteArray` / `fromByteArray`, `toLongArray` / `fromLongArray`) | `12` toByteArray + `3` toLongArray + `15` valueOf (10 byte[], 5 long[]); interop с compressed bitmaps |
| `previousSetBit()` / `previousClearBit()` | `5` / `1` файлов |
| `intersects()` | `2` файла (но `3` в JetBrains + CustomBitSet) |
| Read-only view / immutable variant | `4` independent wrappers в OSS + `1` в JetBrains (syntax MutableBitSet/BitSet) |

### 9.3 Подтверждённые non-goals для core API

| Item | Основание |
|---|---|
| Concurrent variants | 0 в OSS (JetBrains-specific) |
| Compressed storage | Domain-specific (RoaringBitmap, druid) |
| Tri-state / packed bits | 0 в OSS (JetBrains-specific) |
| `xor()` | 0 файлов в OSS, 1 в JetBrains |
| `flip()` | 1+1 файлов (negligible) |

### 9.4 Новый сигнал от OSS

- **`cardinality()` / `size()` disambiguation** критичнее, чем казалось по JetBrains-данным: `54` + `48` файлов. Java's `size()` (=capacity) семантически confusing; stdlib API должен чётко разделить `count` (= set bits) и не экспонировать internal capacity.
- **Kotlin-friendly iteration** подтверждена как #1 ergonomic gap: `51` use-site файл с ручным nextSetBit loop внутри 4b-scope (`105` across full clones) + `7` с stream() workaround.
- **Immutability** — значимый gap: `4` immutable wrappers в OSS + `1` в JetBrains (syntax MutableBitSet/BitSet) = `5` independent implementations. Ещё `3` JetBrains-артефакта — multiplatform reimplementations (не immutability-мотивированные), а `1` OSS impl (graal MultiTypeStateWithBitSet) — domain-specific container с cached cardinality.
