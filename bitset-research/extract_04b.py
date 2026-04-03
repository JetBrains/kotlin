#!/usr/bin/env python3
"""
Step 4b: Extract BitSet usage data from 23 OSS repos.
Discovers files importing java.util.BitSet, classifies them,
extracts normalized method calls, and outputs TSV.

Output: TSV: repo | file | cls | methods | context
"""

import os
import re
import subprocess
import sys
from pathlib import Path

REPOS_DIR = Path("/Users/dmitry.nekrasov/dev/repos/for-bitset-research")

REPOS = [
    "antlr4", "graal", "lucene", "androidx", "guava",
    "spark", "spotbugs", "calcite", "h2database", "checkstyle",
    "pmd", "flink", "netty", "eclipse-collections", "druid",
    "hive", "spring-framework", "hibernate-orm", "RoaringBitmap",
    "commons-lang", "beam", "elasticsearch", "cassandra",
]

# ---- Method detection patterns (reused from extract_03b_v3.py, adapted) ----

KNOWN_METHODS = {
    'BitSet()': [
        r'\bnew\s+BitSet\s*\(\s*\)',
        r'(?<!\w)BitSet\s*\(\s*\)(?!\s*\{)',  # Kotlin constructor
    ],
    'BitSet(int)': [
        r'\bnew\s+BitSet\s*\(\s*[^)]+\s*\)',  # new BitSet(N) — any non-empty arg
        r'(?<!\w)BitSet\s*\(\s*[^)]+\s*\)(?!\s*\{)',  # Kotlin
    ],
    'get(int)': [
        r'\.get\s*\(\s*[^,)]+\s*\)',
    ],
    'get(from,to)': [
        r'\.get\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)',
    ],
    'set(int)': [
        r'\.set\s*\(\s*[^,)]+\s*\)',
    ],
    'set(int,bool)': [
        r'\.set\s*\(\s*[^,)]+\s*,\s*(?:true|false|value|boolean|Boolean|flag|b|enabled|selected|present|visible|state|result|condition|on|off)\s*\)',
    ],
    'set(from,to)': [
        r'\.set\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)',
    ],
    'set(from,to,bool)': [
        r'\.set\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*,\s*[^,)]+\s*\)',
    ],
    'clear()': [
        r'\.clear\s*\(\s*\)',
    ],
    'clear(int)': [
        r'\.clear\s*\(\s*[^,)]+\s*\)',
    ],
    'clear(from,to)': [
        r'\.clear\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)',
    ],
    'flip(int)': [
        r'\.flip\s*\(\s*[^,)]+\s*\)',
    ],
    'flip(from,to)': [
        r'\.flip\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)',
    ],
    'and()': [r'\.and\s*\('],
    'or()': [r'\.or\s*\('],
    'xor()': [r'\.xor\s*\('],
    'andNot()': [r'\.andNot\s*\('],
    'nextSetBit()': [r'\.nextSetBit\s*\('],
    'nextClearBit()': [r'\.nextClearBit\s*\('],
    'previousSetBit()': [r'\.previousSetBit\s*\('],
    'previousClearBit()': [r'\.previousClearBit\s*\('],
    'isEmpty()': [r'\.isEmpty\s*\(\s*\)', r'\.isEmpty\b(?!\s*\()'],  # property or method
    'cardinality()': [r'\.cardinality\s*\('],
    'size()': [r'\.size\s*\(\s*\)'],
    'length()': [r'\.length\s*\(\s*\)'],
    'intersects()': [r'\.intersects\s*\('],
    'toByteArray()': [r'\.toByteArray\s*\('],
    'toLongArray()': [r'\.toLongArray\s*\('],
    'valueOf(long[])': [r'BitSet\s*\.\s*valueOf\s*\('],
    'stream()': [r'\.stream\s*\(\s*\)'],
    'clone()': [r'\.clone\s*\(\s*\)'],
    'equals()': [r'\.equals\s*\('],
    'hashCode()': [r'\.hashCode\s*\('],
    'toString()': [r'\.toString\s*\('],
}

# Priority order for output
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


def discover_files(repo_dir: Path) -> list[str]:
    """Find all files importing java.util.BitSet."""
    try:
        result = subprocess.run(
            ["rg", "-l", r"import java\.util\.BitSet", "--type", "java", "--type", "kotlin",
             str(repo_dir)],
            capture_output=True, text=True, timeout=60
        )
        files = [f.strip() for f in result.stdout.strip().split('\n') if f.strip()]
        return sorted(files)
    except Exception as e:
        print(f"  ERROR discovering files: {e}", file=sys.stderr)
        return []


def strip_comments_and_imports(content: str) -> str:
    """Remove import lines, single-line comments, and block comments."""
    # Remove block comments
    text = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    # Remove single-line comments
    text = re.sub(r'//.*$', '', text, flags=re.MULTILINE)
    # Remove import lines
    text = re.sub(r'^\s*import\s+.*$', '', text, flags=re.MULTILINE)
    return text


def has_bitset_in_body(content: str) -> bool:
    """Check if BitSet appears in code body (not just imports/comments)."""
    stripped = strip_comments_and_imports(content)
    return bool(re.search(r'\bBitSet\b', stripped))


def classify_file(filepath: str, content: str) -> str:
    """Classify file as gen/test/impl/use."""
    fp_lower = filepath.lower()

    # gen: generated code
    gen_path_markers = ['/generated/', '/generated-src/', '/gen-java/', '/gen-javabean/',
                        '/gen-proto/', '/build/generated']
    if any(m in fp_lower for m in gen_path_markers):
        return 'gen'
    # Check first 20 lines for generation markers
    first_lines = '\n'.join(content.split('\n')[:20])
    if re.search(r'@Generated|DO NOT EDIT|Generated by|Auto-generated|Autogenerated', first_lines, re.IGNORECASE):
        return 'gen'

    # test
    test_path_markers = ['/src/test/', '/test/', '/tests/', '/testfixtures/',
                         '/testsrc/', '/testdata/', '/src/test/resources/',
                         '/jmh/', '/benchmark/']
    if any(m in fp_lower for m in test_path_markers):
        return 'test'
    basename = os.path.basename(filepath)
    if re.match(r'.*Test\.(java|kt)$', basename) or re.match(r'.*Tests\.(java|kt)$', basename):
        return 'test'
    if re.match(r'.*IT\.(java|kt)$', basename):  # integration tests
        return 'test'
    if re.match(r'Test.*\.(java|kt)$', basename):
        return 'test'

    # impl: file defines a BitSet-like class
    stripped = strip_comments_and_imports(content)
    # extends BitSet
    if re.search(r'\bextends\s+BitSet\b', stripped):
        return 'impl'
    # class FooBitSet or class BitSetFoo (not test-path)
    if re.search(r'\bclass\s+\w*BitSet\w*\b', stripped):
        # But not if it's just "uses" a BitSet in name like BitSetTest
        class_match = re.search(r'\bclass\s+(\w*BitSet\w*)\b', stripped)
        if class_match:
            cname = class_match.group(1)
            if not cname.endswith('Test') and not cname.endswith('Tests'):
                return 'impl'

    return 'use'


def extract_bitset_code(content: str) -> str:
    """Extract code lines that are likely BitSet-related (for method detection).
    Removes imports and comments, then filters to lines mentioning BitSet or
    known BitSet variable names."""
    stripped = strip_comments_and_imports(content)

    # Find BitSet variable/field/param names
    var_names = set()
    # Java: BitSet foo, BitSet foo = ..., final BitSet foo
    for m in re.finditer(r'\bBitSet\b\s+(\w+)', stripped):
        var_names.add(m.group(1))
    # Kotlin: val/var foo: BitSet, foo: BitSet
    for m in re.finditer(r'(\w+)\s*:\s*BitSet\b', stripped):
        var_names.add(m.group(1))

    # Also scan for lines with BitSet literal
    lines = stripped.split('\n')
    relevant_lines = []
    for line in lines:
        if 'BitSet' in line:
            relevant_lines.append(line)
        elif var_names and any(re.search(r'\b' + re.escape(v) + r'\.', line) for v in var_names):
            relevant_lines.append(line)
    return '\n'.join(relevant_lines)


def extract_methods(content: str) -> list[str]:
    """Extract normalized BitSet method calls from file content."""
    code = extract_bitset_code(content)
    full_stripped = strip_comments_and_imports(content)

    found = set()

    # --- Constructors: scan full code for these ---
    if re.search(r'\bnew\s+BitSet\s*\(\s*\)', full_stripped):
        found.add('BitSet()')
    if re.search(r'\bnew\s+BitSet\s*\(\s*[^)\s]+', full_stripped):
        found.add('BitSet(int)')
    # Kotlin constructor
    if re.search(r'(?<!\w)BitSet\s*\(\s*\)(?!\s*[{:])', full_stripped):
        found.add('BitSet()')
    if re.search(r'(?<!\w)BitSet\s*\(\s*[^){]+\s*\)(?!\s*[{:])', full_stripped):
        found.add('BitSet(int)')

    # --- valueOf ---
    if re.search(r'BitSet\s*\.\s*valueOf\s*\(', full_stripped):
        found.add('valueOf(long[])')

    # --- Method calls: scan relevant code ---
    scan_text = code if code else full_stripped

    # Unambiguous methods (unique to BitSet)
    unambiguous = {
        'nextSetBit()': r'\.nextSetBit\s*\(',
        'nextClearBit()': r'\.nextClearBit\s*\(',
        'previousSetBit()': r'\.previousSetBit\s*\(',
        'previousClearBit()': r'\.previousClearBit\s*\(',
        'andNot()': r'\.andNot\s*\(',
        'cardinality()': r'\.cardinality\s*\(',
        'intersects()': r'\.intersects\s*\(',
        'toByteArray()': r'\.toByteArray\s*\(',
        'toLongArray()': r'\.toLongArray\s*\(',
        'flip(int)': r'\.flip\s*\(\s*[^,)]+\s*\)',
        'flip(from,to)': r'\.flip\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)',
    }
    for method, pat in unambiguous.items():
        if re.search(pat, scan_text):
            found.add(method)

    # Ambiguous methods — check in relevant BitSet code
    # and(): must distinguish from logical &&
    if re.search(r'\.and\s*\(\s*\w', scan_text):
        found.add('and()')
    if re.search(r'\.or\s*\(\s*\w', scan_text):
        found.add('or()')
    if re.search(r'\.xor\s*\(\s*\w', scan_text):
        found.add('xor()')

    # get with args
    if re.search(r'\.get\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)', scan_text):
        found.add('get(from,to)')
    if re.search(r'\.get\s*\(\s*[^,)]+\s*\)(?!\s*,)', scan_text):
        found.add('get(int)')

    # set with args — need to distinguish set(int), set(int,bool), set(from,to), set(from,to,bool)
    # 3-arg set
    if re.search(r'\.set\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*,\s*[^,)]+\s*\)', scan_text):
        found.add('set(from,to,bool)')
    # 2-arg set — could be set(int,bool) or set(from,to)
    BOOL_NAMES = {'true', 'false', 'value', 'val', 'flag', 'b', 'bool',
                  'enabled', 'selected', 'present', 'visible', 'state',
                  'on', 'off', 'set', 'active', 'exists', 'found',
                  'putValue', 'EMPTY_VALUE', 'newValue', 'oldValue'}
    two_arg_sets = re.findall(r'\.set\s*\(\s*([^,)]+)\s*,\s*([^,)]+)\s*\)', scan_text)
    for arg1, arg2 in two_arg_sets:
        arg2_stripped = arg2.strip()
        # Heuristic: if second arg looks boolean-like, it's set(int,bool)
        if arg2_stripped in BOOL_NAMES or arg2_stripped.lower() in BOOL_NAMES:
            found.add('set(int,bool)')
        elif arg2_stripped.startswith('!') or arg2_stripped.endswith('()'):
            # negated expression or method call returning boolean
            found.add('set(int,bool)')
        elif re.match(r'^\d+$', arg2_stripped) or '+' in arg2_stripped or '-' in arg2_stripped:
            # numeric literal or arithmetic expression = range end
            found.add('set(from,to)')
        else:
            # Default: assume set(from,to) for simple identifiers
            # (may over-count set(from,to) vs set(int,bool))
            found.add('set(from,to)')
    # 1-arg set
    if re.search(r'\.set\s*\(\s*[^,)]+\s*\)(?!\s*,)', scan_text):
        found.add('set(int)')

    # clear
    if re.search(r'\.clear\s*\(\s*\)', scan_text):
        found.add('clear()')
    if re.search(r'\.clear\s*\(\s*[^,)]+\s*,\s*[^,)]+\s*\)', scan_text):
        found.add('clear(from,to)')
    if re.search(r'\.clear\s*\(\s*[^,)]+\s*\)(?!\s*,)', scan_text):
        found.add('clear(int)')

    # isEmpty — Java method or Kotlin property
    if re.search(r'\.isEmpty\s*\(\s*\)', scan_text) or re.search(r'\.isEmpty\b(?!\s*\()', scan_text):
        found.add('isEmpty()')

    # size() — must be careful, many things have .size()
    if re.search(r'\.size\s*\(\s*\)', scan_text):
        found.add('size()')
    # Kotlin property .size (only on BitSet-specific lines)
    if re.search(r'\.size\b(?!\s*\()', code):  # only in BitSet-relevant code
        found.add('size()')

    # length()
    if re.search(r'\.length\s*\(\s*\)', scan_text):
        found.add('length()')

    # stream()
    if re.search(r'\.stream\s*\(\s*\)', scan_text):
        found.add('stream()')

    # clone()
    if re.search(r'\.clone\s*\(\s*\)', scan_text):
        found.add('clone()')

    # equals() — only in BitSet-related code
    if re.search(r'\.equals\s*\(', code):
        found.add('equals()')

    # hashCode() — only in BitSet-related code
    if re.search(r'\.hashCode\s*\(', code):
        found.add('hashCode()')

    # toString() — only in BitSet-related code
    if re.search(r'\.toString\s*\(', code):
        found.add('toString()')

    # Sort by canonical order
    result = [m for m in METHOD_ORDER if m in found]
    for m in sorted(found):
        if m not in result:
            result.append(m)
    return result


def extract_context(filepath: str, content: str) -> str:
    """Extract a 1-line context description."""
    # Try class-level Javadoc
    m = re.search(r'/\*\*\s*\n\s*\*\s*(.+?)(?:\n|\*/)', content)
    if m:
        ctx = m.group(1).strip().rstrip('.')
        if len(ctx) > 100:
            ctx = ctx[:97] + '...'
        return ctx

    # Try class declaration
    m = re.search(r'(?:public|internal|abstract|final)\s+(?:class|interface|enum|object)\s+(\w+)', content)
    if m:
        return m.group(0).strip()

    # Fallback: filename
    return os.path.basename(filepath)


def relative_path(filepath: str, repo_dir: Path) -> str:
    """Get path relative to repo directory."""
    try:
        return str(Path(filepath).relative_to(repo_dir))
    except ValueError:
        return filepath


def process_repo(repo_name: str) -> list[dict]:
    """Process a single repository and return list of file records."""
    repo_dir = REPOS_DIR / repo_name
    if not repo_dir.exists():
        print(f"  WARNING: repo dir not found: {repo_dir}", file=sys.stderr)
        return []

    files = discover_files(repo_dir)
    print(f"  {repo_name}: {len(files)} files with import", file=sys.stderr)

    records = []
    excluded = 0
    for filepath in files:
        try:
            with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
                content = f.read()
        except Exception as e:
            print(f"    ERROR reading {filepath}: {e}", file=sys.stderr)
            continue

        # False positive filter
        if not has_bitset_in_body(content):
            excluded += 1
            continue

        cls = classify_file(filepath, content)
        methods = []
        if cls in ('use', 'impl'):
            methods = extract_methods(content)
        context = extract_context(filepath, content)
        rel_path = relative_path(filepath, repo_dir)

        records.append({
            'repo': repo_name,
            'file': rel_path,
            'cls': cls,
            'methods': '; '.join(methods),
            'context': context.replace('\t', ' ').replace('\n', ' '),
        })

    if excluded:
        print(f"    excluded {excluded} false positives", file=sys.stderr)

    return records


def main():
    all_records = []
    for repo_name in sorted(REPOS):
        print(f"Processing {repo_name}...", file=sys.stderr)
        records = process_repo(repo_name)
        all_records.extend(records)

    # Output TSV
    print("repo\tfile\tcls\tmethods\tcontext")
    for r in all_records:
        print(f"{r['repo']}\t{r['file']}\t{r['cls']}\t{r['methods']}\t{r['context']}")

    # Summary to stderr
    total = len(all_records)
    by_cls = {}
    for r in all_records:
        by_cls[r['cls']] = by_cls.get(r['cls'], 0) + 1
    by_repo = {}
    for r in all_records:
        by_repo[r['repo']] = by_repo.get(r['repo'], 0) + 1

    print(f"\n=== SUMMARY ===", file=sys.stderr)
    print(f"Total files: {total}", file=sys.stderr)
    print(f"By classification: {by_cls}", file=sys.stderr)
    print(f"By repo: {by_repo}", file=sys.stderr)


if __name__ == '__main__':
    main()
