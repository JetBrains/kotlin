# BitSet Research — Index

Индекс результатов исследования для добавления мультиплатформенного BitSet в Kotlin stdlib (KT-55163).
План исследования: [`AI/bitset-research-plan.md`](../bitset-research-plan.md).

## Статус шагов

| Шаг | Название | Статус | Файл |
|---|---|---|---|
| 1 | Ревизия существующих реализаций в экосистеме Kotlin | **Выполнен** | [step-01-kotlin-implementations.md](step-01-kotlin-implementations.md) |
| 2 | Кросс-языковой обзор API | **Выполнен** | [step-02-cross-language.md](step-02-cross-language.md) |
| 3a | Сбор данных: использование BitSet в репозитории Kotlin | **Выполнен** | [step-03a-kotlin-repo-data.md](step-03a-kotlin-repo-data.md) |
| 3b | Сбор данных: использование BitSet в репозитории IntelliJ Community | **Выполнен** | [step-03b-intellij-repo-data.md](step-03b-intellij-repo-data.md) |
| 3c | Анализ: частотная таблица, классификация паттернов и каталог обёрток | **Выполнен** | [step-03c-analysis.md](step-03c-analysis.md) |
| 4a | Отбор репозиториев и методология | **Выполнен** | [step-04a-repo-selection.md](step-04a-repo-selection.md) |
| 4b | Извлечение данных по репозиториям | **Выполнен** | [step-04b-repo-data.md](step-04b-repo-data.md) |
| 4c | Сводный анализ open-source использования | **Выполнен** | [step-04c-analysis.md](step-04c-analysis.md) |
| 5 | Болевые точки Java BitSet и wish list сообщества | Не начат | — |
| 6 | Таксономия use cases | Не начат | — |
| 7 | Анализ KEEP процесса | Не начат | — |
| 8 | Фреймворк дизайн-решений | Не начат | — |
| 9 | Связь с EnumSet и deprecation стратегия | Не начат | — |
| 10 | Мультиплатформенная стратегия | Не начат | — |
| 11 | Сверка дизайн-решений | Не начат | — |
| 12 | Требования к производительности | Не начат | — |
| 13 | API-прототипирование | Не начат | — |
| 14 | Финальная консолидация | Не начат | — |

## Краткие итоги выполненных шагов

### Шаг 1 (Выполнен)

Изучены 6 реализаций: `java.util.BitSet` (baseline), `kotlin.native.BitSet`, Wasm internal BitSet, `CustomBitSet`, `BitSetUtil.kt`, и 2 сторонние библиотеки (BitVector, KmpIO). Большинство мутабельные; BitVector — единственная библиотека с immutable/mutable разделением. BitVector реализует `Iterable<Int>`; остальные не реализуют интерфейсы коллекций. Ключевые пробелы Native BitSet: нет `cardinality()`, `copy()`, итерации, `valueOf()`, конвертации. Ключевые Kotlin-идиомы: `operator []`, `IntRange`, `forEachBit {}`, initializer-конструктор.

### Шаг 2 (Выполнен)

Проанализированы BitSet-реализации в 5 языках (C++, Rust, C#, Scala, Swift) + Java из шага 1. Современные реализации тяготеют к модели «множество целых чисел» (Scala `Set[Int]`, Swift `SetAlgebra`, Rust fixedbitset) вместо «вектора бит». Scala — единственный язык с полноценным immutable/mutable split. Swift предлагает value-type с CoW и отдельный `BitArray`. C# `BitArray` — пример антипаттернов (нет value equality, итерация по bool). Ключевые выводы: интеграция с коллекциями — ожидание пользователей; итерация по set-битам (индексы) — доминирующий паттерн; read-only/mutable split совместим с паттерном Kotlin collections. Детальные артефакты: [`step-02-cpp-bitset.md`](step-02-cpp-bitset.md), [`step-02-rust-bitvec-fixedbitset.md`](step-02-rust-bitvec-fixedbitset.md), [`step-02-csharp-bitarray.md`](step-02-csharp-bitarray.md), [`step-02-scala-bitset.md`](step-02-scala-bitset.md), [`step-02-swift-bitset.md`](step-02-swift-bitset.md).

### Шаг 3a (Выполнен)

Каталогизированы все 25 исходных файлов + 1 API dump, содержащих BitSet-связанный код в репозитории kotlin. Распределение по подсистемам: compiler backend (12 файлов — JVM, Common IR, K/N), analysis/symbol light classes (5 файлов), stdlib regex engine (2 файла), K/N runtime impl + tests (3 файла), native commonizer (1 файл), утилиты (1 файл), API dump (1 файл). Доминируют два контекста: dataflow/liveness analysis в компиляторе (BitSet как множество переменных/типов/контейнеров) и character class operations в regex-движке (BitSet как множество code points). Три BitSet-варианта используются параллельно: `java.util.BitSet` (14 файлов), `CustomBitSet` (4 файла), `kotlin.native.BitSet` (4 файла). Сырой каталог без классификации — анализ будет выполнен в шаге 3c.

### Шаг 3b (Выполнен)

Каталогизированы все 211 файлов в репозитории intellij-community, содержащих BitSet-связанный код. Обнаружено 11 кастомных BitSet-реализаций/обёрток: `ConcurrentBitSet` (thread-safe, VarHandle), `ConcurrentThreeStateBitSet` (true/false/null), `BitSetAsRAIntContainer`, `IdBitSet` (смещённые ID), `com.intellij.util.diff.BitSet` и `fleet.util.BitSet` (обе — копии K/N stdlib), `MutableBitSet`/`BitSet` (syntax, immutable/mutable), `UnsignedBitSet` (отрицательные индексы), `BitSetFlags` (адаптер Flags), `BitSet32` (mslinks, 32-бит). Два файла содержат прямые ссылки на KT-55163. Использования организованы по 38 подсистемам: platform/util (28), java-decompiler (27), diff-impl (19), vcs-log (19), python (16, вкл. 13 сгенерированных), groovy (11), vcs-impl (11), lang-impl (10), java-analysis-impl (10), analysis-impl (7), и ~20 подсистем с 1-5 файлами. Сырой каталог без классификации — анализ будет выполнен в шаге 3c.

### Шаг 3c (Выполнен)

В [`step-03c-analysis.md`](step-03c-analysis.md) сведены в один артефакт частоты методов, оценка стабильности top-10, классификация `171` TSV-level use-site entries (raw `180` file-level loci) по 10 паттернам и каталог wrapper/utility-слоя вокруг `BitSet`. Combined-профиль доминируется `get(int)`, `set(from,to)`, `BitSet()`, `set(int)` и `BitSet(int)`, а главные API gaps устойчиво повторяются в нескольких подсистемах: typed `copy()`, итерация по set-битам, range/navigation operations и Kotlin-friendly query semantics. Concurrent/tri-state/sparse wrappers зафиксированы как специализированные надстройки, а не как обязательная часть core stdlib API.

### Шаг 4a (Выполнен)

Отобраны 23 внешних open-source репозитория с подтверждённым наличием ≥3 файлов с упоминанием `java.util.BitSet` в code search. Из 11 seed-кандидатов (план исследования) прошли верификацию 7; ещё 16 отобраны через curated domain-driven поиск (broad search через GitHub Code Search использовался как эвристический discovery без per-repo верификации). Покрыто 11 доменов: compilers/JVM, search/indexing, Android, JVM libraries, data processing, static analysis, database engines, networking, collections, web frameworks, ORM. Медиана звёзд — 8 535, суммарно ~423k. Единственный Kotlin-first репозиторий — `androidx/androidx`. 8 из 23 репозиториев достигли лимита `--limit=30` по умолчанию — реальное число файлов выше. Методология поиска, rate limit management и ограничения выборки задокументированы.

### Шаг 4b (Выполнен)

Извлечены данные об использовании `java.util.BitSet` из 23 open-source репозиториев. Каталогизировано 641 файл: 422 use, 23 impl, 178 test, 18 gen; ещё 51 файл исключён (import без использования в коде). Top-5 методов в use-site файлах: `get(int)` (253), `set(int)` (243), `BitSet()` (168), `BitSet(int)` (119), `nextSetBit()` (67). Обнаружено 23 impl-файла с повторяющимися gaps: immutability (5 реализаций: calcite, hibernate, druid, graal FinalBitSet, graal MultiTypeState), iteration/Iterable (4: calcite ImmutableBitSet, graal Util, druid WrappedImmutableBitSetBitmap, calcite BitSets), serialization (3: beam BitSetCoder ×2, graal RecordedOperationPersistence), compressed storage (3: RoaringBitmap), fluent/convenience API (3: commons-lang FluentBitSet, netty Match, spotbugs MethodBytecodeSet). Арифметическая согласованность проверена: сумма per-repo use-sites = total use-sites для каждого метода.

### Шаг 4c (Выполнен)

В [`step-04c-analysis.md`](step-04c-analysis.md) сведены частотная таблица `33` методов, оценка стабильности (leave-one-out + incremental), классификация `422` use-sites по `12` паттернам и каталог «неудобных» паттернов. Top-5 (`get(int)` 253, `set(int)` 243, `BitSet()` 168, `BitSet(int)` 119, `nextSetBit()` 67) стабилен с шага 2 из 21 (incremental). Top-10 membership нестабильна в LOO для 4 repos (ранги 8–11 тесно сгруппированы: разброс 6 файлов). Крупнейшие паттерны: Dataflow/liveness (86, 20.4%), Column/parameter mask (72, 17.1%), Static analysis detectors (67, 15.9%), Query optimization (49, 11.6%). Главные «неудобные» паттерны: ручной nextSetBit-цикл (105 файлов), 5 independent immutability wrappers, 19 файлов с unsafe `clone()` cast. Сравнение с JetBrains (step-03c): top-3 core совпадает; `set(from,to)` #2 в JetBrains → #12 в OSS (IntelliJ diff/bytecode specific); `cardinality()` #10 → #6 (OSS больше считают). OSS подтверждает must-have core API: get/set, constructors, nextSetBit + iteration, or/and/andNot, isEmpty/cardinality, typed copy(), equals/hashCode.
