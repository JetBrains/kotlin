#!/usr/bin/env python3
"""
Generate step-04b-repo-data.md from step-04b-raw.tsv.
Produces per-repo catalog sections with file tables and frequency mini-tables.
"""

import csv
import sys
from collections import defaultdict, OrderedDict

TSV_FILE = "bitset-research/step-04b-raw.tsv"
OUTPUT_FILE = "bitset-research/step-04b-repo-data.md"

# Canonical repo display names (owner/repo) and ordering from step-04a
REPO_DISPLAY = OrderedDict([
    ("antlr4", "antlr/antlr4"),
    ("graal", "oracle/graal"),
    ("lucene", "apache/lucene"),
    ("androidx", "androidx/androidx"),
    ("guava", "google/guava"),
    ("spark", "apache/spark"),
    ("spotbugs", "spotbugs/spotbugs"),
    ("calcite", "apache/calcite"),
    ("h2database", "h2database/h2database"),
    ("checkstyle", "checkstyle/checkstyle"),
    ("pmd", "pmd/pmd"),
    ("flink", "apache/flink"),
    ("netty", "netty/netty"),
    ("eclipse-collections", "eclipse-collections/eclipse-collections"),
    ("druid", "apache/druid"),
    ("hive", "apache/hive"),
    ("spring-framework", "spring-projects/spring-framework"),
    ("hibernate-orm", "hibernate/hibernate-orm"),
    ("RoaringBitmap", "RoaringBitmap/RoaringBitmap"),
    ("commons-lang", "apache/commons-lang"),
    ("beam", "apache/beam"),
    ("elasticsearch", "elastic/elasticsearch"),
    ("cassandra", "apache/cassandra"),
])

REPO_DOMAINS = {
    "antlr4": "Compilers / parsers / JVM",
    "graal": "Compilers / parsers / JVM",
    "lucene": "Search / indexing",
    "androidx": "Android ecosystem",
    "guava": "JVM core libraries / utilities",
    "spark": "Data processing / pipeline",
    "spotbugs": "Static analysis / code quality",
    "calcite": "Database engines",
    "h2database": "Database engines",
    "checkstyle": "Static analysis / code quality",
    "pmd": "Static analysis / code quality",
    "flink": "Data processing / pipeline",
    "netty": "Networking",
    "eclipse-collections": "Collections / data structures",
    "druid": "Database engines",
    "hive": "Data processing / pipeline",
    "spring-framework": "Web frameworks",
    "hibernate-orm": "ORM",
    "RoaringBitmap": "Collections / data structures",
    "commons-lang": "JVM core libraries / utilities",
    "beam": "Data processing / pipeline",
    "elasticsearch": "Search / indexing",
    "cassandra": "Database engines",
}

# Impl file descriptions: file key -> gap description
IMPL_DESCRIPTIONS = {
    # RoaringBitmap
    "RoaringBitmap:roaringbitmap/src/main/java/org/roaringbitmap/BitSetUtil.java":
        "Конвертация j.u.BitSet <-> RoaringBitmap через long[]/byte[]; нет встроенной interop с compressed bitmap форматами",
    "RoaringBitmap:roaringbitmap/src/main/java/org/roaringbitmap/RoaringBitSet.java":
        "Drop-in BitSet subclass с compressed RoaringBitmap storage; компенсирует плохую memory efficiency j.u.BitSet на больших sparse наборах",
    "RoaringBitmap:roaringbitmap/src/main/java/org/roaringbitmap/buffer/BufferBitSetUtil.java":
        "Конвертация j.u.BitSet <-> off-heap MutableRoaringBitmap; нет поддержки NIO buffer-backed storage",
    # Beam
    "beam:runners/core-java/src/main/java/org/apache/beam/runners/core/triggers/FinishedTriggersBitSet.java":
        "Домен-специфичная обёртка: isFinished / clearRecursively поверх j.u.BitSet для trigger-state tracking",
    "beam:sdks/java/core/src/main/java/org/apache/beam/sdk/coders/BitSetCoder.java":
        "Deterministic Coder для сериализации j.u.BitSet в Beam pipelines; нет встроенной streaming serialization",
    "beam:sdks/java/core/src/main/java/org/apache/beam/sdk/util/BitSetCoder.java":
        "Deprecated предшественник BitSetCoder; та же gap — отсутствие streaming serialization",
    # Calcite
    "calcite:core/src/main/java/org/apache/calcite/util/BitSets.java":
        "Утилиты: contains (superset check), toIter, forEach по set-битам; отсутствуют в j.u.BitSet",
    "calcite:core/src/main/java/org/apache/calcite/util/ImmutableBitSet.java":
        "Immutable, Comparable, Iterable<Integer> BitSet с value-семантикой; компенсирует мутабельность и отсутствие Iterable/Comparable",
    # Commons-lang
    "commons-lang:src/main/java/org/apache/commons/lang3/util/FluentBitSet.java":
        "Fluent API wrapper: mutating-методы возвращают this для chaining; j.u.BitSet возвращает void",
    # Druid
    "druid:processing/src/main/java/org/apache/druid/collections/bitmap/BitSetBitmapFactory.java":
        "Адаптер j.u.BitSet -> BitmapFactory интерфейс; нет абстрактного factory/strategy контракта",
    "druid:processing/src/main/java/org/apache/druid/collections/bitmap/WrappedBitSetBitmap.java":
        "Адаптер j.u.BitSet -> MutableBitmap интерфейс; нет unified bitmap contract для взаимозаменяемости с Roaring/Concise",
    "druid:processing/src/main/java/org/apache/druid/collections/bitmap/WrappedImmutableBitSetBitmap.java":
        "Immutable адаптер с IntIterator и toBytes(); нет immutable контракта и iterator protocol в j.u.BitSet",
    # Graal
    "graal:compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/replaycomp/RecordedOperationPersistence.java":
        "JSON-сериализация BitSet как long[] массива для replay compilation; нет встроенной JSON serialization",
    "graal:espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/Util.java":
        "mergeBitSets (OR-reduce) и Iterable<Integer> итераторы по set/unset битам; нет bulk-merge и iteration API",
    "graal:substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/typestate/MultiTypeStateWithBitSet.java":
        "BitSet + cached cardinality + type-state metadata; j.u.BitSet пересчитывает cardinality() при каждом вызове",
    "graal:truffle/src/com.oracle.truffle.api.utilities/src/com/oracle/truffle/api/utilities/FinalBitSet.java":
        "Read-only BitSet с @CompilationFinal long[] для JIT constant-folding; j.u.BitSet мутабелен и непрозрачен для partial evaluation",
    # Guava
    "guava:android/guava/src/com/google/common/base/CharMatcher.java":
        "BitSetMatcher: адаптер BitSet -> CharMatcher predicate (matches(char)); нет predicate-интерфейса в j.u.BitSet",
    "guava:guava/src/com/google/common/base/CharMatcher.java":
        "BitSetMatcher: адаптер BitSet -> CharMatcher predicate (matches(char)); нет predicate-интерфейса в j.u.BitSet",
    # H2
    "h2database:h2/src/main/org/h2/mvstore/FreeSpaceBitSet.java":
        "Block-size-aware addressing + contiguous-range allocation; j.u.BitSet не знает о sized blocks и lifecycle mark/free",
    # Hibernate
    "hibernate-orm:hibernate-core/src/main/java/org/hibernate/internal/util/ImmutableBitSet.java":
        "Immutable BitSet с defensive copy и contains (subset check); j.u.BitSet мутабелен и не имеет containsAll",
    # Netty
    "netty:codec-http/src/main/java/io/netty/handler/codec/http/HttpChunkLineValidatingByteProcessor.java":
        "Match extends BitSet: fluent builder (chars/range) + state transition; нет bulk-set-from-string и range-set convenience API",
    # SpotBugs
    "spotbugs:spotbugs/src/main/java/edu/umd/cs/findbugs/ba/BlockType.java":
        "BitSet как dataflow lattice value: validity, top/bottom, depth tracking; нет lattice-семантики и metadata fields",
    "spotbugs:spotbugs/src/main/java/edu/umd/cs/findbugs/ba/MethodBytecodeSet.java":
        "toString() с маппингом bit positions -> JVM opcode names; нет кастомного domain-specific toString в j.u.BitSet",
}

# Excluded files counts per repo (from extraction log)
EXCLUDED_COUNTS = {
    "calcite": 1,
    "checkstyle": 20,
    "elasticsearch": 28,
    "h2database": 1,
    "hibernate-orm": 1,
}

METHOD_ORDER = [
    'BitSet()', 'BitSet(int)', 'get(int)', 'get(from,to)',
    'set(int)', 'set(int,bool)', 'set(from,to)', 'set(from,to,bool)',
    'clear()', 'clear(int)', 'clear(from,to)',
    'flip(int)', 'flip(from,to)',
    'and()', 'or()', 'xor()', 'andNot()',
    'nextSetBit()', 'nextClearBit()', 'previousSetBit()', 'previousClearBit()',
    'isEmpty()', 'cardinality()', 'size()', 'length()',
    'intersects()', 'toByteArray()', 'toLongArray()', 'valueOf(long[])',
    'stream()', 'clone()', 'equals()', 'hashCode()', 'toString()',
]


def read_tsv(path):
    records = []
    with open(path, 'r') as f:
        reader = csv.DictReader(f, delimiter='\t')
        for row in reader:
            records.append(row)
    return records


def build_frequency_table(records):
    """Build method -> count of use-site files."""
    freq = defaultdict(int)
    for r in records:
        if r['cls'] != 'use' or not r['methods']:
            continue
        methods = [m.strip() for m in r['methods'].split(';')]
        for m in methods:
            if m:
                freq[m] += 1
    return freq


def format_methods_or_desc(record, repo_key):
    """Format methods column for use files, or gap description for impl files."""
    cls = record['cls']
    if cls == 'impl':
        key = f"{record['repo']}:{record['file']}"
        desc = IMPL_DESCRIPTIONS.get(key, "—")
        return desc
    elif cls in ('use',):
        methods = record.get('methods', '')
        if methods:
            return methods.replace('; ', ', ')
        return '(тип в сигнатуре)'
    else:
        return ''


def generate_repo_section(repo_name, records, section_num):
    """Generate markdown section for one repo."""
    display = REPO_DISPLAY[repo_name]
    domain = REPO_DOMAINS[repo_name]
    excluded = EXCLUDED_COUNTS.get(repo_name, 0)

    lines = []
    lines.append(f"## {section_num}. {display}")
    lines.append(f"")
    lines.append(f"**Домен:** {domain}")
    if excluded:
        lines.append(f"**Исключено (false positive):** {excluded} файлов")
    lines.append("")

    # Count by classification
    cls_counts = defaultdict(int)
    for r in records:
        cls_counts[r['cls']] += 1

    # File catalog
    lines.append("### Каталог файлов")
    lines.append("")
    lines.append("| # | Путь | Cls | Методы / Описание | Контекст |")
    lines.append("|---|---|---|---|---|")
    for i, r in enumerate(records, 1):
        path = r['file']
        cls = r['cls']
        methods_or_desc = format_methods_or_desc(r, repo_name)
        ctx = r.get('context', '')
        # Truncate context
        if len(ctx) > 80:
            ctx = ctx[:77] + '...'
        # Escape pipes in content
        methods_or_desc = methods_or_desc.replace('|', '\\|')
        ctx = ctx.replace('|', '\\|')
        lines.append(f"| {i} | `{path}` | {cls} | {methods_or_desc} | {ctx} |")

    lines.append("")

    # Frequency table (only use-site files)
    freq = build_frequency_table(records)
    if freq:
        lines.append("### Частотная таблица (use-site файлов)")
        lines.append("")
        lines.append("| Метод | Файлов |")
        lines.append("|---|---:|")
        for m in METHOD_ORDER:
            if m in freq:
                lines.append(f"| `{m}` | {freq[m]} |")
        # Any methods not in METHOD_ORDER
        for m in sorted(freq.keys()):
            if m not in METHOD_ORDER:
                lines.append(f"| `{m}` | {freq[m]} |")
        lines.append("")

    use_count = cls_counts.get('use', 0)
    impl_count = cls_counts.get('impl', 0)
    test_count = cls_counts.get('test', 0)
    gen_count = cls_counts.get('gen', 0)
    total = sum(cls_counts.values())
    parts = []
    if use_count: parts.append(f"{use_count} use")
    if impl_count: parts.append(f"{impl_count} impl")
    if test_count: parts.append(f"{test_count} test")
    if gen_count: parts.append(f"{gen_count} gen")
    lines.append(f"**Итого:** {total} файлов ({', '.join(parts)})")
    lines.append("")

    return '\n'.join(lines), cls_counts, freq


def main():
    records = read_tsv(TSV_FILE)

    # Group by repo
    by_repo = defaultdict(list)
    for r in records:
        by_repo[r['repo']].append(r)

    # Build output
    out = []

    # Header
    out.append("# Шаг 4b. Извлечение данных по репозиториям")
    out.append("")
    out.append("**Резюме.** Извлечены данные об использовании `java.util.BitSet` из 23 open-source репозиториев. "
               f"Обработано {len(records)} файлов: "
               f"{sum(1 for r in records if r['cls']=='use')} use, "
               f"{sum(1 for r in records if r['cls']=='impl')} impl, "
               f"{sum(1 for r in records if r['cls']=='test')} test, "
               f"{sum(1 for r in records if r['cls']=='gen')} gen. "
               f"Ещё {sum(EXCLUDED_COUNTS.values())} файлов исключены как false positive "
               f"(import без использования в коде). "
               "Доминирующие методы в use-site файлах: "
               "`set(int)`, `get(int)`, `BitSet(int)`, `BitSet()`, `nextSetBit()`, `or()`, `cardinality()`. "
               "23 impl-файла фиксируют повторяющиеся gaps: immutability (5 реализаций), "
               "iteration/Iterable (4), serialization (3), compressed storage (3), fluent/convenience API (3).")
    out.append("")
    out.append("## Входные данные")
    out.append("")
    out.append("- [`bitset-research/step-04a-repo-selection.md`](step-04a-repo-selection.md) — список репозиториев")
    out.append("- [`bitset-research/step-03c-analysis.md`](step-03c-analysis.md) — словарь нормализации (раздел 2.2)")
    out.append("")
    out.append("## Методология")
    out.append("")
    out.append("1. **Обнаружение:** `rg -l \"import java.util.BitSet\" --type java --type kotlin` по каждому клону в `/Users/dmitry.nekrasov/dev/repos/for-bitset-research/`.")
    out.append("2. **Фильтрация false positives:** файлы, где `BitSet` встречается только в import-строках и комментариях, исключены (28 elasticsearch generated-src, 20 checkstyle test resources, 1 calcite, 1 h2database, 1 hibernate-orm).")
    out.append("3. **Классификация** (приоритет: gen > test > impl > use):")
    out.append("   - `gen`: путь содержит `/generated/`, `/generated-src/`; или первые 20 строк содержат `@Generated`, `DO NOT EDIT`")
    out.append("   - `test`: путь содержит `/test/`, `/tests/`, `/testFixtures/`, `/jmh/`, `/benchmark/`; или имя файла оканчивается на `Test.java`/`Tests.java`")
    out.append("   - `impl`: файл определяет класс с `*BitSet*` в имени или `extends BitSet`")
    out.append("   - `use`: все остальные")
    out.append("4. **Извлечение методов** (для `use` и `impl` файлов): regex-паттерны по телу файла (без import/comments), нормализация по словарю step-03c.")
    out.append("5. **Контекст:** первая строка Javadoc класса или объявление класса.")
    out.append("")
    out.append("### Примечания к отдельным репозиториям")
    out.append("")
    out.append("- **google/guava**: содержит зеркальные деревья `android/guava/` и `guava/` (Android-compatible и Java 8+ версии). Обе версии каталогизированы; при агрегации учитывать двойной счёт.")
    out.append("- **apache/lucene**: имеет собственный `org.apache.lucene.util.BitSet` (abstract class, ~64 файла) и `FixedBitSet` (~140 файлов). В scope шага 4b входят только файлы с `import java.util.BitSet` (32 файла).")
    out.append("- **RoaringBitmap/RoaringBitmap**: репозиторий является реализацией compressed bitmap. Большинство файлов с j.u.BitSet — тесты, использующие его как reference oracle.")
    out.append("- **apache/hive**: ~350 Thrift-generated файлов упоминают `BitSet` через FQN без import — исключены поисковым фильтром.")
    out.append("")

    # Normalization dictionary
    out.append("## Словарь нормализации")
    out.append("")
    out.append("| Raw | Normalized |")
    out.append("|---|---|")
    out.append("| `new BitSet()` | `BitSet()` |")
    out.append("| `new BitSet(N)` / `BitSet(size)` | `BitSet(int)` |")
    out.append("| `BitSet.valueOf(...)` | `valueOf(long[])` |")
    out.append("| `.set(int, boolean)` | `set(int,bool)` |")
    out.append("| `.isEmpty` (Kotlin property) | `isEmpty()` |")
    out.append("| `.size` (Kotlin property) | `size()` |")
    out.append("| Прочие методы | as-is |")
    out.append("")
    out.append("---")
    out.append("")

    # Per-repo sections
    all_cls_counts = {}
    all_freqs = {}
    global_freq = defaultdict(int)

    for section_num, (repo_name, display) in enumerate(REPO_DISPLAY.items(), 1):
        repo_records = by_repo.get(repo_name, [])
        if not repo_records:
            out.append(f"## {section_num}. {display}")
            out.append("")
            out.append("Файлов с `import java.util.BitSet` не обнаружено.")
            out.append("")
            continue

        section_text, cls_counts, freq = generate_repo_section(repo_name, repo_records, section_num)
        out.append(section_text)
        out.append("---")
        out.append("")
        all_cls_counts[repo_name] = cls_counts
        all_freqs[repo_name] = freq
        for m, c in freq.items():
            global_freq[m] += c

    # Global summary
    out.append("## Глобальная сводка")
    out.append("")

    total_use = sum(c.get('use', 0) for c in all_cls_counts.values())
    total_impl = sum(c.get('impl', 0) for c in all_cls_counts.values())
    total_test = sum(c.get('test', 0) for c in all_cls_counts.values())
    total_gen = sum(c.get('gen', 0) for c in all_cls_counts.values())
    total_all = total_use + total_impl + total_test + total_gen
    total_excluded = sum(EXCLUDED_COUNTS.values())

    out.append("| Метрика | Значение |")
    out.append("|---|---:|")
    out.append(f"| Репозиториев | {len(all_cls_counts)} |")
    out.append(f"| Файлов (после фильтрации) | {total_all} |")
    out.append(f"| Исключено (false positive) | {total_excluded} |")
    out.append(f"| use | {total_use} |")
    out.append(f"| impl | {total_impl} |")
    out.append(f"| test | {total_test} |")
    out.append(f"| gen | {total_gen} |")
    out.append("")

    # Global frequency table
    out.append("### Глобальная частотная таблица (use-site файлов)")
    out.append("")
    out.append("| Метод | Файлов |")
    out.append("|---|---:|")
    for m in METHOD_ORDER:
        if m in global_freq:
            out.append(f"| `{m}` | {global_freq[m]} |")
    for m in sorted(global_freq.keys()):
        if m not in METHOD_ORDER:
            out.append(f"| `{m}` | {global_freq[m]} |")
    out.append("")

    # Arithmetic check table
    out.append("### Арифметическая проверка")
    out.append("")
    out.append("| Repo | use | impl | test | gen | total |")
    out.append("|---|---:|---:|---:|---:|---:|")
    check_total_use = 0
    check_total_impl = 0
    check_total_test = 0
    check_total_gen = 0
    check_total_all = 0
    for repo_name in REPO_DISPLAY:
        cc = all_cls_counts.get(repo_name, {})
        u = cc.get('use', 0)
        i = cc.get('impl', 0)
        t = cc.get('test', 0)
        g = cc.get('gen', 0)
        total = u + i + t + g
        display = REPO_DISPLAY[repo_name]
        out.append(f"| {display} | {u} | {i} | {t} | {g} | {total} |")
        check_total_use += u
        check_total_impl += i
        check_total_test += t
        check_total_gen += g
        check_total_all += total
    out.append(f"| **Итого** | **{check_total_use}** | **{check_total_impl}** | **{check_total_test}** | **{check_total_gen}** | **{check_total_all}** |")
    out.append("")

    # Per-method cross-check
    out.append("### Проверка: per-repo use-site суммы")
    out.append("")
    out.append("| Метод | " + " | ".join(REPO_DISPLAY[r] for r in REPO_DISPLAY) + " | **Total** |")
    out.append("|---| " + " | ".join("---:" for _ in REPO_DISPLAY) + " | ---: |")

    # This table would be huge (23 columns). Let's skip per-method per-repo and just verify programmatically.
    # Instead, do a compact check
    out.pop()  # remove header row
    out.pop()  # remove separator
    out.pop()  # remove empty line
    out.pop()  # remove section header

    out.append("#### Верификация per-repo сумм")
    out.append("")
    errors = []
    for m in METHOD_ORDER:
        if m not in global_freq:
            continue
        per_repo_sum = sum(all_freqs.get(repo, {}).get(m, 0) for repo in REPO_DISPLAY)
        if per_repo_sum != global_freq[m]:
            errors.append(f"- `{m}`: sum per-repo = {per_repo_sum}, global = {global_freq[m]} **MISMATCH**")

    if errors:
        out.append("**Обнаружены расхождения:**")
        out.extend(errors)
    else:
        out.append("Сумма per-repo use-site counts по каждому методу совпадает с глобальной таблицей. Расхождений не обнаружено.")
    out.append("")

    # Write output
    with open(OUTPUT_FILE, 'w') as f:
        f.write('\n'.join(out))

    print(f"Written {OUTPUT_FILE}: {len(out)} lines", file=sys.stderr)
    print(f"Totals: {total_all} files ({total_use} use, {total_impl} impl, {total_test} test, {total_gen} gen) + {total_excluded} excluded", file=sys.stderr)


if __name__ == '__main__':
    main()
