# Промпты для Claude Code: Step 3c (Вариант А — с предобработкой)

## Предварительный шаг: скопировать файлы

Скопируй `step-03b-extracted.tsv` и `extract_03b_v3.py` в `bitset-research/`.

TSV-файл содержит предобработанные данные из step-03b: для каждого файла — classification (use/impl/test/gen/FP), access mode (J=JVM-direct, W=wrapper-mediated), и нормализованные BitSet-методы. Скрипт `extract_03b_v3.py` — канонический extractor для воспроизводимости (заменил предыдущие версии).

---

## Промпт 1: Частотная таблица

```
Контекст: я выполняю Шаг 3c из bitset-research/bitset-research-plan.md — аналитический шаг поверх сырых данных из 3a и 3b.

Входные данные:
* bitset-research/step-03a-kotlin-repo-data.md (20 use-sites, маленький файл)
* bitset-research/step-03b-extracted.tsv (предобработанные данные IntelliJ — ~6K токенов, TSV: file|cls|mode|methods)
* bitset-research/step-03b-intellij-repo-data.md (сырой каталог — для переклассификации cls=use→impl и коррекции методов)

TSV-формат step-03b-extracted.tsv:
- cls: use=use-site, impl=implementation, test=тест, gen=generated, FP=false-positive
- mode: J=JVM-direct (java.util.BitSet), W=wrapper-mediated, ?=unknown
- methods: нормализованные имена BitSet-методов через запятую. Пустое поле = pass-through (файл передаёт BitSet, но не вызывает его API напрямую).

Задача: построить объединённую частотную таблицу.

### Шаг 0: Зафиксируй scope
- Из TSV: сначала сверить с `step-03b-intellij-repo-data.md` и перевести записи, которые в сыром каталоге описаны как самостоятельные реализации/обёртки BitSet, из `cls=use` в `cls=impl` (текущее состояние: `7` таких записей — `BitSetAsRAIntContainer.java`, `text-matching/BitSet.kt`, `MutableBitSet.kt`, `UnsignedBitSet.java`, `BitSetFlags.java`, `BitSet32.java`, `fleet.util.BitSet.kt`). После переклассификации — посчитай use-sites (`cls=use`). Запиши точное число.
- Method-level correction pre-pass: для каждой `cls=use` TSV-строки сверить поле `methods` с сырым каталогом `step-03b-intellij-repo-data.md` и при необходимости с исходным кодом; удалить ложные методы, если TSV-extractor ошибочно захватил вызовы не-BitSet объектов. Известный кейс: `FilesScanExecutor.kt` — `deque.size` относится к `ConcurrentLinkedDeque`, а не к BitSet; `size()` исключить.
- Из step-03a: определи use-site файлы (исключи реализации BitSet, тесты, API-дампы). Запиши число.
- Из step-03a извлеки per-file method lists (нормализуй аналогично TSV).

### Шаг 1: Частотная таблица
Для каждого метода (BitSet(), get(int), set(int), ...) посчитай:
- Kotlin repo count: сколько use-site файлов из step-03a используют этот метод
- IntelliJ repo count: сколько use-site файлов из TSV (cls=use, methods содержит этот метод)
- IntelliJ JVM-direct: подсчёт только среди mode=J
- IntelliJ Wrapper-mediated: подсчёт только среди mode=W
- Total = Kotlin + IntelliJ

Столбцы: Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total
Сортировка по Total desc.
Группировка по семействам: Construction, Single-bit, Range, Bulk bitwise, Navigation, Query, Conversion, Equality, Other.

Traceability:
- Для методов с ≤10 use-sites — перечисли все файлы
- Для >10 — count + 2–3 representative files

Верификация:
- Kotlin + IntelliJ = Total
- IntelliJ-J + IntelliJ-W = IntelliJ (для каждого метода)
- Отдельно отметь pass-through файлы (файлы с пустыми методами среди `cls=use` после переклассификации) — они входят в общее число use-sites, но не вносят вклад в частоты

Результат: bitset-research/step-03c-part1-frequencies.md

Ultrathink.
```

---

## Промпт 2: Стабильность + классификация паттернов

```
Контекст: продолжаю Шаг 3c из bitset-research/bitset-research-plan.md.

Входные данные:
* bitset-research/step-03c-part1-frequencies.md (из предыдущего шага)
* bitset-research/step-03b-extracted.tsv
* bitset-research/step-03a-kotlin-repo-data.md
* bitset-research/step-03b-intellij-repo-data.md (для expansion синтетических TSV-строк в file-level loci)

### Шаг 2: Оценка стабильности
- Сравни top-10 по Kotlin-only vs Combined.
- Нестабильна, если: меняется состав top-10 методов или порядок top-5 (формальный критерий из `bitset-research-plan.md`). Дополнительный диагностический сигнал: сильные сдвиги рангов между соседними парами.
- Если нестабильна: выбери до 2 доп. открытых JetBrains-репозиториев, быстрый анализ top-10, пересчитай. Зафиксировать commit SHA и дату среза для каждого добранного репозитория.
- Задокументируй: полностью / частично / не стабилизировалась.

### Шаг 3: Классификация use-sites по паттернам
Категории:
1. Dataflow / liveness analysis
2. Set membership / visited tracking
3. Diff / text comparison
4. Character class / lexer automata
5. Bytecode offset tracking
6. Parameter mask / overload generation
7. Flag storage
8. Serialization protocol
9. Graph algorithms
10. Indexing / file ID containers

Pre-pass: развернуть `3` синтетические TSV-строки секции 39 `step-03b-intellij-repo-data.md` (`API dumps (7 файлов)`, `JDK API version files (4 файла)`, `JDK аннотации (1 файл)`) в `12` individual file-level loci; итого `180` file-level use-site loci.

Правила:
- Каждый use-site locus (`cls=use` file-level entries из развёрнутого TSV + use-sites из step-03a, итого `180`) — ровно одна категория.
- Pass-through файлы тоже классифицируй (по контексту, не по методам).
- Если две категории имеют одинаковый method profile — объедини с пометкой.
- Для каждой категории: список файлов, критические методы, count.
- Верификация: total classified = `180` file-level use-site loci (после expansion `3` синтетических TSV-строк в `12` individual loci). NB: `part1` считает частоты на `171` TSV-row entries, `part2` классифицирует `180` file-level loci после expansion.

Результат: bitset-research/step-03c-part2-stability-patterns.md

Ultrathink.
```

---

## Промпт 3: Каталог обёрток + финальная сборка

```
Контекст: завершаю Шаг 3c из bitset-research/bitset-research-plan.md.

Входные данные:
* bitset-research/step-03c-part1-frequencies.md
* bitset-research/step-03c-part2-stability-patterns.md
* bitset-research/step-03b-extracted.tsv (для impl entries)
* bitset-research/step-03a-kotlin-repo-data.md

### Шаг 4: Каталог обёрток
Для каждой обёртки (cls=impl в TSV, включая переклассифицированные из cls=use на шаге 0 промпта 1, + обёртки и реализации из step-03a, включая `CustomBitSet` как самостоятельную реализацию):
- Что это и где живёт
- Какой пробел в стандартном API компенсирует
- Какие BitSet-методы использует/оборачивает/расширяет
- Рекомендация: что включить в stdlib API

### Шаг 5: Сборка step-03c-analysis.md

Структура:
1. **Summary** — top-5 методов, доминирующие паттерны, главные пробелы
2. **Scope & Methodology** — числа use-sites, критерии, описание предобработки TSV
3. **Frequency Table** — из part1
4. **Stability Assessment** — из part2
5. **Usage Pattern Classification** — из part2
6. **Wrapper / Utility Catalog** — из шага 4
7. **Implications for API Design** — must-have / nice-to-have / gap analysis

Верификация финального документа:
- Все счётчики согласованы; total classified = `180` file-level loci; сумма категорий = `180`
- Каждый use-site locus — ровно в одной категории
- Каждый метод прослеживается до файлов
- Проценты считаются от `180` (file-level), а не от `171` (TSV-row level)

Промежуточные файлы (part1, part2) НЕ удалять: финальный `step-03c-analysis.md` ссылается на них как на входные данные, их наличие обеспечивает traceability и воспроизводимость ревью-цикла.
Результат: bitset-research/step-03c-analysis.md

Ultrathink.
```
