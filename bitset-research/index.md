# BitSet Research — Index

Индекс результатов исследования для добавления мультиплатформенного BitSet в Kotlin stdlib (KT-55163).
План исследования: [`AI/bitset-research-plan.md`](../bitset-research-plan.md).

## Статус шагов

| Шаг | Название | Статус | Файл |
|---|---|---|---|
| 1 | Ревизия существующих реализаций в экосистеме Kotlin | **Выполнен** | [step-01-kotlin-implementations.md](step-01-kotlin-implementations.md) |
| 2 | Кросс-языковой обзор API | **Выполнен** | [step-02-cross-language.md](step-02-cross-language.md) |
| 3 | Глубокий анализ использования в репозиториях JetBrains | Не начат | — |
| 4 | Широкий анализ open-source использования | Не начат | — |
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
