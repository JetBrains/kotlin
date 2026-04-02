# Шаг 3c. Сводный анализ использования BitSet в кодовой базе JetBrains

## Резюме

Сведены в один артефакт частотный анализ, оценка стабильности, классификация use-sites и каталог обёрток/utility-слоя вокруг `BitSet`. В объединённой выборке из `178` use-sites доминируют `get(int)` (`75`), `set(from,to)` (`63`), `BitSet()` (`60`), `set(int)` (`52`) и `BitSet(int)` (`33`), а крупнейшие паттерны использования — dataflow/liveness (`35`), diff/text comparison (`27`) и bytecode offset tracking (`26`).

Главные API gaps повторяются в нескольких независимых слоях: отсутствуют Kotlin-friendly `copy()`, итерация по set-битам, удобные range-операции и ясная query-семантика для `count`/`length`. Напротив, concurrent/tri-state/packed wrappers выглядят специализированными надстройками, а не сигналом к расширению core stdlib BitSet.

## Входные данные

- `bitset-research/step-03c-part1-frequencies.md`
- `bitset-research/step-03c-part2-stability-patterns.md`
- `bitset-research/step-03b-extracted.tsv`
- `bitset-research/step-03a-kotlin-repo-data.md`

## 1. Summary

- Scope: `20` Kotlin use-site файлов + `158` IntelliJ TSV use-site entries = `178` use-sites.
- Method-bearing subset: `147` use-sites; pass-through subset: `31`.
- Top-5 combined methods: `get(int)` (`75`), `set(from,to)` (`63`), `BitSet()` (`60`), `set(int)` (`52`), `BitSet(int)` (`33`).
- Largest usage patterns: `Dataflow / liveness analysis` (`35`), `Diff / text comparison` (`27`), `Bytecode offset tracking` (`26`). Top-3 категории дают `88 / 178` use-sites (`49.4%`); top-5 дают `130 / 178` (`73.0%`).
- Stability verdict: `top-10` и даже `top-5` membership не стабилизируются при переходе от Kotlin-only к combined выборке; быстрый добор `JetBrains/Grammar-Kit` и `JetBrains/markdown` порядок лидеров не меняет.
- Wrapper verdict: повторяются четыре системных gaps — typed copy, set-bit iteration, range operations, functional-style derivation from mutable masks. Отдельные concurrent/sparse/tri-state wrappers выглядят как domain-specific adapters.

## 2. Scope & Methodology

### 2.1 Scope

- IntelliJ TSV дал `158` строк `cls=use`, из них `129` с непустым `methods` и `29` pass-through.
- Kotlin часть дала `20` use-site файлов после исключения `4` реализаций BitSet, `1` test suite и `1` API dump; из них `18` method-bearing и `2` pass-through.
- Отдельно для каталога обёрток рассмотрены `8` строк `cls=impl` из TSV и wrapper/utility-слой из `step-03a`.

### 2.2 Нормализация

Чтобы объединить `step-03a` и TSV в один словарь методов, использовались те же правила, что в `step-03c-part1-frequencies.md`:

- `new BitSet(int)` / `BitSet(size)` / `CustomBitSet(nodesCount)` -> `BitSet(int)`
- `CustomBitSet()` -> `BitSet()`
- `[int]` -> `get(int)`
- `[int] = ...` и `.set(int, Boolean)` -> `set(int,bool)`
- свойства `.isEmpty` / `.size` -> `isEmpty()` / `size()`
- `CustomBitSet.valueOf(LongArray)` -> `valueOf(LongArray)`

IntelliJ-колонки трактуются так:

- `IntelliJ-J`: `mode=J`, прямое использование `java.util.BitSet`
- `IntelliJ-W`: `mode=W`, wrapper-mediated использование или кастомный BitSet-тип
- `IntelliJ`: сумма `J` и `W` для method-bearing строк

В TSV есть `4` строки `mode=?`, но все они пустые по `methods` и относятся к pass-through/synthetic entries, поэтому method frequencies полностью раскладываются на `J` и `W`.

### 2.3 Верификация

- Частотная таблица была пересчитана программно из `20` Kotlin per-file method lists и `158` TSV use-site rows; расхождений с `step-03c-part1-frequencies.md` по всем `34` методам не найдено.
- Проверка масштаба выполнена: `20 + 158 = 178` use-sites; `18 + 129 = 147` method-bearing; `2 + 29 = 31` pass-through.
- Сумма counts по `10` категориям из `step-03c-part2-stability-patterns.md` даёт `178`, что согласуется с total scope.
- Уникальность категоризации не пересобиралась заново, а перенята из `step-03c-part2-stability-patterns.md`; в итоговый документ не вносилось новых переклассификаций use-sites.
- Для каталога обёрток `8` TSV impl rows сгруппированы в `5` концептуальных wrapper families, чтобы не дублировать interface/implementation пары.

## 3. Frequency Table

Ниже — сводная частотная таблица, отсортированная по `Total`. Она консолидирует grouped tables из `step-03c-part1-frequencies.md` в один список.

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `get(int)` | 13 | 62 | 44 | 18 | 75 |
| `set(from,to)` | 2 | 61 | 49 | 12 | 63 |
| `BitSet()` | 7 | 53 | 50 | 3 | 60 |
| `set(int)` | 8 | 44 | 34 | 10 | 52 |
| `BitSet(int)` | 9 | 24 | 18 | 6 | 33 |
| `set(int,bool)` | 6 | 26 | 16 | 10 | 32 |
| `nextSetBit()` | 3 | 26 | 25 | 1 | 29 |
| `or()` | 10 | 11 | 11 | 0 | 21 |
| `isEmpty()` | 3 | 15 | 14 | 1 | 18 |
| `cardinality()` | 3 | 13 | 11 | 2 | 16 |
| `clear()` | 2 | 10 | 4 | 6 | 12 |
| `size()` | 3 | 6 | 1 | 5 | 9 |
| `clear(int)` | 3 | 6 | 3 | 3 | 9 |
| `equals()` | 5 | 4 | 4 | 0 | 9 |
| `copy()` | 7 | 0 | 0 | 0 | 7 |
| `length()` | 1 | 6 | 6 | 0 | 7 |
| `clone()` | 2 | 5 | 4 | 1 | 7 |
| `forEachBit()` | 6 | 0 | 0 | 0 | 6 |
| `set(from,to,bool)` | 1 | 5 | 3 | 2 | 6 |
| `nextClearBit()` | 1 | 5 | 5 | 0 | 6 |
| `stream()` | 0 | 6 | 6 | 0 | 6 |
| `andNot()` | 4 | 1 | 1 | 0 | 5 |
| `and()` | 4 | 0 | 0 | 0 | 4 |
| `toString()` | 1 | 2 | 2 | 0 | 3 |
| `hashCode()` | 2 | 1 | 1 | 0 | 3 |
| `intersects()` | 3 | 0 | 0 | 0 | 3 |
| `toByteArray()` | 0 | 2 | 2 | 0 | 2 |
| `mapEachBit()` | 1 | 0 | 0 | 0 | 1 |
| `orWithFilterHasChanged()` | 1 | 0 | 0 | 0 | 1 |
| `valueOf(LongArray)` | 1 | 0 | 0 | 0 | 1 |
| `forEachWord()` | 1 | 0 | 0 | 0 | 1 |
| `xor()` | 1 | 0 | 0 | 0 | 1 |
| `set(IntRange)` | 1 | 0 | 0 | 0 | 1 |
| `toLongArray()` | 0 | 1 | 1 | 0 | 1 |

### Наблюдения

- Combined выборка доминируется не bulk-операциями, а базовыми access/update primitives: `get`, `set(range)`, конструкторы и `set(int)`.
- `set(from,to)` — главный внешний корректор Kotlin-only профиля: в Kotlin он встречается лишь `2` раза, а в combined выборке поднимается на `2` место с `63` use-sites.
- Kotlin-specific ergonomic layer хорошо виден по методам, отсутствующим в IntelliJ-части: `copy()` (`7`), `forEachBit()` (`6`), `mapEachBit()` (`1`), `set(IntRange)` (`1`), `valueOf(LongArray)` (`1`), `orWithFilterHasChanged()` (`1`), `forEachWord()` (`1`).
- В IntelliJ-половине значимым JVM-only хвостом остаются `stream()`, `toByteArray()`, `toLongArray()` и `length()`, то есть либо Java interop, либо low-level serialization/debug helpers.

## 4. Stability Assessment

### 4.1 Kotlin-only vs Combined

| Rank | Kotlin-only | Count | Combined | Count |
|---|---|---:|---|---:|
| 1 | `get(int)` | 13 | `get(int)` | 75 |
| 2 | `or()` | 10 | `set(from,to)` | 63 |
| 3 | `BitSet(int)` | 9 | `BitSet()` | 60 |
| 4 | `set(int)` | 8 | `set(int)` | 52 |
| 5 | `BitSet()` | 7 | `BitSet(int)` | 33 |
| 6 | `copy()` | 7 | `set(int,bool)` | 32 |
| 7 | `forEachBit()` | 6 | `nextSetBit()` | 29 |
| 8 | `set(int,bool)` | 6 | `or()` | 21 |
| 9 | `equals()` | 5 | `isEmpty()` | 18 |
| 10 | `and()` | 4 | `cardinality()` | 16 |

### 4.2 Быстрый добор двух открытых JetBrains-репозиториев

- `JetBrains/Grammar-Kit`: `3` включённых use-sites, `1` исключённый.
- `JetBrains/markdown`: `2` включённых use-sites, `5` исключённых.
- Совокупный вклад добора: `5` релевантных use-sites.

### 4.3 Combined + 2 repos

| Rank | Combined + 2 repos | Count | Delta vs Combined |
|---|---|---:|---:|
| 1 | `get(int)` | 80 | +5 |
| 2 | `set(from,to)` | 63 | +0 |
| 3 | `BitSet()` | 60 | +0 |
| 4 | `set(int)` | 53 | +1 |
| 5 | `BitSet(int)` | 38 | +5 |
| 6 | `set(int,bool)` | 36 | +4 |
| 7 | `nextSetBit()` | 29 | +0 |
| 8 | `or()` | 21 | +0 |
| 9 | `isEmpty()` | 19 | +1 |
| 10 | `cardinality()` | 17 | +1 |

### 4.4 Вывод

- Статус: `не стабилизировалась`.
- `top-5 membership` уже меняется на переходе от Kotlin-only к combined выборке: `or()` выпадает, `set(from,to)` входит.
- Быстрый добор добавляет массу доминирующим методам, но не меняет порядок expanded top-10. Это означает, что локальная JetBrains-выборка уже достаточно хорошо фиксирует лидеров combined-профиля, но Kotlin-only профиль недостаточен для ранжирования must-have API.

## 5. Usage Pattern Classification

| Категория | Count | Critical methods |
|---|---:|---|
| Dataflow / liveness analysis | 35 | `get(int)` (23), `BitSet()` (18), `set(int)` (17), `nextSetBit()` (10), `or()` (9) |
| Set membership / visited tracking | 10 | `BitSet(int)` (5), `BitSet()` (4), `get(int)` (3), `or()` (3), `isEmpty()` (3) |
| Diff / text comparison | 27 | `set(from,to)` (19), `BitSet()` (10), `nextSetBit()` (8), `nextClearBit()` (5), `get(int)` (4) |
| Character class / regex | 5 | `get(int)` (4), `BitSet()` (2), `BitSet(int)` (2), `set(int,bool)` (2), `nextSetBit()` (2) |
| Bytecode offset tracking | 26 | `set(from,to)` (16), `BitSet()` (7), `nextSetBit()` (5), `get(int)` (3), `set(int)` (3) |
| Parameter mask / overload generation | 8 | `get(int)` (5), `BitSet(int)` (4), `set(int)` (2), `isEmpty()` (2), `length()` (2) |
| Flag storage | 16 | `get(int)` (8), `set(int,bool)` (7), `set(from,to)` (7), `BitSet()` (6), `set(int)` (4) |
| Serialization protocol | 9 | `BitSet()` (4), `set(from,to)` (3), `set(int)` (2), `BitSet(int)` (2), `set(int,bool)` (2) |
| Graph algorithms | 20 | `get(int)` (11), `set(int,bool)` (9), `set(from,to)` (8), `BitSet()` (4), `forEachBit()` (2) |
| Indexing / file ID containers | 22 | `set(int)` (16), `get(int)` (13), `clear()` (6), `BitSet()` (5), `size()` (5) |

### Наблюдения

- `Dataflow / liveness analysis`, `Graph algorithms` и `Indexing / file ID containers` вместе дают `77` use-sites. Это кластер, где важнее всего single-bit updates, merge-операции и set-bit iteration.
- `Diff / text comparison`, `Bytecode offset tracking` и `Character class / regex` вместе дают `58` use-sites. Это range-heavy кластер, в котором критичны `set(from,to)`, `nextSetBit()` и `nextClearBit()`.
- `Parameter mask / overload generation` и `Flag storage` дают ещё `24` use-sites и подчёркивают роль `copy()/clone()`, `length()`, `intersects()` и value equality для mask-like сценариев.
- Совпадающих method profiles, которые требовали бы слияния категорий, не обнаружено; в итоговую сводку перенесена та же `10`-категорийная схема, что и в `step-03c-part2-stability-patterns.md`.

## 6. Wrapper / Utility Catalog

### 6.1 Wrapper families из `step-03b-extracted.tsv` (`cls=impl`)

`8` impl rows логически сводятся к `5` wrapper families.

| Wrapper family | Что это и где живёт | Какой gap компенсирует | BitSet surface | Рекомендация для stdlib API |
|---|---|---|---|---|
| `ConcurrentBitSet` + `ConcurrentBitSetImpl` | Thread-safe аналог `java.util.BitSet` в `platform/util/concurrency/src/com/intellij/util/containers/`. Реализован на `int[]` + `VarHandle`, оптимизирован под read-heavy concurrent access. | У стандартного `BitSet` нет thread-safe варианта и нет чёткого контракта для concurrent readers/writers. | `set`, `set(index,bool)`, `clear`, `get`, `nextSetBit`, `nextClearBit`, `size`, `cardinality`, `toIntArray`, `toString`. | Не включать concurrent semantics в core stdlib BitSet. Но сами query/navigation primitives (`nextSetBit`, `nextClearBit`, `cardinality`, `clear`) должны быть в core, чтобы поверх них можно было строить специализированные concurrent wrappers. |
| `ConcurrentPackedBitsArray` + `ConcurrentPackedBitsArrayImpl` | Атомарный контейнер для bit-chunks по `1..32` бита в `platform/util/concurrency/...`. Физически построен поверх `ConcurrentBitSetImpl`. | BitSet хранит только один флаг на индекс; части IntelliJ нужен компактный atomic storage для нескольких связанных флагов на один id. | Публично: `get`, `set`, `clear`. Внутри использует word-level access `ConcurrentBitSetImpl`. | Не сигнал к расширению BitSet API. Это отдельная структура для packed flags, не core BitSet. |
| `ConcurrentThreeStateBitSet` + `ConcurrentThreeStateBitSetImpl` | Tri-state wrapper (`true` / `false` / `null`) в `platform/util/concurrency/...`, кодирует каждый логический индекс двумя битами поверх `ConcurrentBitSet`. | Обычный BitSet не умеет представлять nullable boolean state. | Публично: `get`, `set`, `compareAndSet`, `clear`, `size`; внутренняя реализация делегирует на `set(index,bool)`/`get(index)`/`clear()`. | Не включать tri-state semantics в core BitSet. Если такой сценарий понадобится, он должен жить как отдельная надстройка над базовым mutable BitSet. |
| `com.intellij.util.diff.BitSet` | Internal BitSet в `platform/util/diff/src/com/intellij/util/diff/BitSet.kt`, прямо помеченный как copy из Kotlin/Native stdlib. Используется diff/LCS-алгоритмами. | Нет доступного Kotlin-first BitSet с range-операциями, двусторонней навигацией и low-level word interop; из-за этого пришлось локально скопировать реализацию. | `BitSet(int)`, `set(from,to)`, `set(from,to,bool)`, `set(range)`, `clear`, `nextSetBit`, `nextClearBit`, `previousSetBit`, `previousClearBit`, `and`, `or`, `xor`, `andNot`, `intersects`, `cardinality`, `toLongArray`, `hashCode`, `toString`. | Самый сильный аргумент в пользу публичного multiplatform stdlib BitSet. Must-have: constructors, range set/clear, `nextSetBit`, `nextClearBit`, `cardinality`, `intersects`, equality/hashCode, typed copy/word interop. `previous*` и `xor` выглядят как strong nice-to-have. |
| `IdBitSet` | Internal container в `platform/util/src/com/intellij/util/indexing/containers/IdBitSet.java`, который хранит id относительно `min(id)` и тем самым экономит память на больших sparse id. | `java.util.BitSet` плохо подходит для наборов с большими id и умеренной шириной диапазона: память зависит от максимального индекса, а не от ширины окна. | В терминах BitSet-профиля: `BitSet(int)`, `get(int)`/`contains`, `nextSetBit`; поверх этого реализует `add`, `remove`, `iterator`, `size`. | Это не gap core BitSet, а отдельный data-structure use case. В stdlib BitSet стоит оставить dense semantics; sparse/id-relative контейнер — отдельная тема. |

### 6.2 Utility / adapter layer из `step-03a-kotlin-repo-data.md`

| Utility family | Что это и где живёт | Какой gap компенсирует | BitSet surface | Рекомендация для stdlib API |
|---|---|---|---|---|
| `BitSetUtil.kt` | Общий utility-файл в `compiler/util/src/org/jetbrains/kotlin/utils/BitSetUtil.kt` для `java.util.BitSet`. | У `java.util.BitSet` неудобные `clone()`/manual loops и нет Kotlin-friendly iteration API. | Добавляет `copy()`, `forEachBit {}`, `mapEachBit {}`; внутри использует `BitSet(size)`, `or()`, `nextSetBit()`. | `copy()` и set-bit iteration должны быть must-have в stdlib API. На JVM это снимет необходимость в локальных extension-файлах почти сразу. |
| `symbolLightUtils.copy` + `copyAndModify` | Local helper layer в `analysis/symbol-light-classes/.../symbolLightUtils.kt` и `.../symbolLightClassUtils.kt` для mask generation при `@JvmOverloads`. | Для безопасного порождения новых масок из старых приходится вручную писать `clone() as BitSet` и локальные copy-helpers. | `copy()`, `copyAndModify {}`, плюс интенсивное использование `set(from,to)`, `clear(int)`, `get(int)`, `isEmpty`, `intersects`, `length`, `equals`, `hashCode`. | Typed `copy()` — must-have. `length()` и `intersects()` выглядят как минимум nice-to-have для mask-like сценариев. |
| `withBit` / `withOutBit` / `withSetBit` | Copy-on-write helpers в `compiler/ir/backend.common/.../LivenessAnalysis.kt` и `kotlin-native/.../StaticInitializersOptimization.kt`. | Анализы хотят функциональный стиль "вернуть новую маску, если изменилось", а не мутировать входной объект по месту. | `get`, `copy`, `set`, `clear`, `or`, `andNot`. | Базовому типу нужен дешёвый и typed `copy()`. Non-mutating helpers можно оставить extension-слоем, но они повторяются достаточно часто, чтобы предусмотреть идиоматичный путь поверх core API. |
| `CustomBitSet` helper layer (`format`, `forEachBitInBoth`) | Тонкие helpers в `kotlin-native/.../DevirtualizationAnalysis.kt` вокруг internal `CustomBitSet`. | Нужны удобная итерация по set-битам и быстрый обход пересечения двух bitsets. | `forEachBit()`, `cardinality()`, `intersects()`, `forEachWord()`, `orWithFilterHasChanged()`. | В core stdlib стоит покрыть iteration/intersection primitives. Специфичные fixed-point helpers вроде `orWithFilterHasChanged()` и debug-formatting — не core API. |

### 6.3 Сводный вывод по wrapper layer

- Повторяющийся adapter-pattern почти всегда сводится к одному из трёх мотивов: `copy()`, set-bit iteration, range/navigation helpers.
- Единственный wrapper, который действительно выглядит как "недостающий stdlib BitSet", — `com.intellij.util.diff.BitSet`: это прямой локальный fork более удобного BitSet API.
- Остальные families либо domain-specific (`IdBitSet`, packed bits, tri-state), либо строятся поверх уже существующего mutable core и потому не требуют раздувать stdlib surface.

## 7. Implications for API Design

### 7.1 Must-have

- Публичный multiplatform mutable core с `BitSet()` и `BitSet(initialCapacity)` или эквивалентными фабриками.
- Базовые single-bit операции: `get`, `set`, `clear`, `set(index, value)`.
- Range operations: как минимум `set(from,to)` / `clear(from,to)`; `set(IntRange)` можно давать как Kotlin-friendly sugar поверх них.
- Навигация и iteration: `nextSetBit` обязателен, `nextClearBit` очень желателен; поверх них нужен Kotlin-friendly обход set-битов (`forEachBit`, iterator, `asSequence()` или аналог).
- Bulk algebra: `or`, `and`, `andNot`, `intersects`; `xor` выглядит полезным, но не так критичен, как первые три.
- Query surface с ясной семантикой: `isEmpty`, `count`/`cardinality`, отдельная сущность для "последний set-бит + 1" (`length`/`lastSetBit + 1`). Java-style двусмысленный `size()` как public semantic API лучше не повторять.
- Typed `copy()` и value equality / `hashCode()`.

### 7.2 Nice-to-have

- `previousSetBit` и `previousClearBit`: они не частотные на use-site уровне, но наличие их в diff-wrapper показывает реальную потребность в двусторонней навигации.
- Low-level interop: `toLongArray()` / `fromLongArray()` или близкий API для serialization/debug/algorithmic reuse.
- `IntRange` overloads и initializer-based factory/constructor как Kotlin-first sugar.
- Read-only / mutable split или хотя бы хорошо поддержанный extension-слой для immutable-style derived operations.

### 7.3 Gap analysis

- Главный gap не в exotic algorithms, а в everyday ergonomics: локальные `copy()`, `clone() as BitSet`, `forEachBit {}`, `copyAndModify {}` появляются в независимых подсистемах.
- Второй gap — platform placement. Наличие `com.intellij.util.diff.BitSet`, скопированного из Kotlin/Native stdlib, показывает, что полезный BitSet API уже существует, но недоступен там, где он реально нужен.
- Range-heavy use cases составляют почти треть выборки (`27` diff + `26` bytecode + `5` regex = `58` use-sites), поэтому без range/navigation support API будет систематически недопокрыт.
- Dataflow/graph/indexing cluster (`35 + 20 + 22 = 77` use-sites) требует эффективных точечных обновлений, merges и обхода set-битов; API, ориентированный только на "bit vector as storage", будет слишком слабым.

### 7.4 Что не выглядит core stdlib API

- Concurrent variants (`ConcurrentBitSet`, packed bits, tri-state wrappers).
- Sparse/id-relative representations (`IdBitSet`).
- Fixed-point-specific helpers (`orWithFilterHasChanged()`).
- Debug/string-formatting helpers как отдельные API-концепции.

Практический итог: первая версия stdlib BitSet должна закрыть частые и повторяемые gaps вокруг обычного mutable dense bitset, а не пытаться включить специализированные wrappers из всех экосистемных ниш.
