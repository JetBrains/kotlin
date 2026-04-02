#!/usr/bin/env python3
"""
Extract compact use-site data from step-03b-intellij-repo-data.md.
Version 3: hybrid - normalized names + raw fallback for uncaptured methods.
Output: TSV: file | class | mode | methods
"""

import re
import sys

KNOWN_METHODS = {
    'BitSet()': [r'new BitSet\(\)', r'BitSet\(\)(?!\()', r'= BitSet\(\)'],
    'BitSet(int)': [r'new BitSet\(\d', r'BitSet\(Int\)', r'BitSet\(int\)', r'BitSet\(\w+\)', r'BitSet\(size', r'BitSet\(length', r'BitSet\(count'],
    'get(int)': [r'\.get\(', r'\[.*\].*BitSet', r'operator.*get'],
    'set(int)': [r'\.set\(\w+\)(?!\s*,)'],
    'set(int,bool)': [r'\.set\(\w+\s*,\s*(?:true|false|value|boolean|Boolean)\)', r'\.set\(int,\s*boolean\)', r'\.set\(Int,\s*Boolean\)'],
    'clear(int)': [r'\.clear\(\w+\)(?!\s*,)', r'\.clear\(pos\)', r'\.clear\(index\)'],
    'flip(int)': [r'\.flip\(\w+\)(?!\s*,)'],
    'set(from,to)': [r'\.set\(\w+\s*,\s*\w+\)(?!\s*,)', r'range.*set', r'\.set\(start', r'\.set\(from', r'\.set\(line1'],
    'set(from,to,bool)': [r'\.set\(\w+\s*,\s*\w+\s*,', r'set\(Int, Int, Boolean\)'],
    'clear(from,to)': [r'\.clear\(\w+\s*,\s*\w+\)'],
    'flip(from,to)': [r'\.flip\(\w+\s*,\s*\w+\)'],
    'get(from,to)': [r'\.get\(\w+\s*,\s*\w+\)'],
    'and()': [r'\.and\('],
    'or()': [r'\.or\('],
    'xor()': [r'\.xor\('],
    'andNot()': [r'\.andNot\('],
    'nextSetBit()': [r'nextSetBit'],
    'nextClearBit()': [r'nextClearBit'],
    'previousSetBit()': [r'previousSetBit'],
    'previousClearBit()': [r'previousClearBit'],
    'isEmpty()': [r'\.isEmpty\b', r'isEmpty\(\)'],
    'cardinality()': [r'cardinality'],
    'size()': [r'\.size\(\)', r'\.size\b'],
    'length()': [r'\.length\(\)', r'\.length\b'],
    'intersects()': [r'intersects\('],
    'toByteArray()': [r'toByteArray'],
    'toLongArray()': [r'toLongArray'],
    'stream()': [r'\.stream\(\)'],
    'clone()': [r'\.clone\(\)', r'\.copy\(\)'],
    'equals()': [r'\.equals\(', r'equals\(\)'],
    'hashCode()': [r'\.hashCode\(', r'hashCode\(\)'],
    'toString()': [r'\.toString\(', r'toString\(\)'],
    'clear()': [r'\.clear\(\)(?!.*int)', r'clear\(\)'],
}

METHOD_ORDER = list(KNOWN_METHODS.keys())


def parse_entries(text: str):
    entries = []
    parts = re.split(r'^(####\s+.+)$', text, flags=re.MULTILINE)
    i = 1
    while i < len(parts) - 1:
        entries.append((parts[i].strip(), parts[i + 1] if i + 1 < len(parts) else ""))
        i += 2
    return entries


def classify(heading: str, body: str) -> str:
    b = body.lower()
    if 'false positive' in b:
        return 'FP'
    if '(тест)' in heading or '/testsrc/' in heading.lower() or '/testdata/' in heading.lower():
        return 'test'
    if '**тип:** кастомная реализация' in b:
        return 'impl'
    if 'сгенерированн' in b and ('jflex' in b or 'thrift' in b):
        return 'gen'
    return 'use'


def access_mode(body: str) -> str:
    b = body.lower()
    m = re.search(r'\*\*BitSet type:\*\*\s*(.+?)(?:\n|$)', body)
    bt = m.group(1).lower() if m else ''
    if 'java.util.bitset' in bt and 'обёртка' not in bt:
        return 'J'
    if any(x in bt for x in ['concurrent', 'diff.bitset', 'bitsetflags', 'unsigned',
                              'idbitset', 'mutable', 'bitsetasra', 'bitset32', 'fleet',
                              'text.matching', 'кастомн', 'обёрт', 'адаптер', 'typealias', 'косвенн']):
        return 'W'
    if 'import java.util.bitset' in b:
        return 'J'
    if 'косвенн' in b:
        return 'W'
    return '?'


def extract_methods(body: str) -> str:
    """Search the entire entry body for BitSet method patterns."""
    found = set()
    
    # Get raw methods field for more targeted search
    m = re.search(r'\*\*Методы:\*\*\s*(.+?)(?:\n\*\*|\n---|\n####|\Z)', body, re.DOTALL)
    methods_text = m.group(1) if m else ''
    
    # Also check Определяемые методы for impl entries
    m2 = re.search(r'\*\*Определяемые методы:\*\*\s*(.+?)(?:\n\*\*|\n---|\n####|\Z)', body, re.DOTALL)
    defined_text = m2.group(1) if m2 else ''
    
    # Also check Контекст
    m3 = re.search(r'\*\*Контекст:\*\*\s*(.+?)(?:\n\*\*|\n---|\n####|\Z)', body, re.DOTALL)
    context_text = m3.group(1) if m3 else ''
    
    search_text = methods_text + ' ' + context_text
    
    for method_name, patterns in KNOWN_METHODS.items():
        for pat in patterns:
            if re.search(pat, search_text, re.IGNORECASE):
                found.add(method_name)
                break
    
    # For impl entries, also search defined methods
    if defined_text:
        for method_name, patterns in KNOWN_METHODS.items():
            for pat in patterns:
                if re.search(pat, defined_text, re.IGNORECASE):
                    found.add(method_name)
                    break
    
    # Sort by canonical order
    result = [m for m in METHOD_ORDER if m in found]
    for m in sorted(found):
        if m not in result:
            result.append(m)
    return ', '.join(result)


def filepath(heading: str) -> str:
    p = heading.lstrip('#').strip()
    return re.sub(r'\s*\(тест\)\s*$', '', p).strip()


def main():
    infile = sys.argv[1] if len(sys.argv) > 1 else '/mnt/user-data/uploads/step-03b-intellij-repo-data.md'
    with open(infile, 'r') as f:
        text = f.read()
    
    entries = parse_entries(text)
    
    lines = ["file\tcls\tmode\tmethods"]
    counts = {}
    
    for heading, body in entries:
        fp = filepath(heading)
        cls = classify(heading, body)
        mode = access_mode(body)
        methods = extract_methods(body) if cls in ('use', 'impl') else ''
        
        counts[cls] = counts.get(cls, 0) + 1
        lines.append(f"{fp}\t{cls}\t{mode}\t{methods}")
    
    print('\n'.join(lines))
    
    # Summary
    total_use = counts.get('use', 0)
    with_methods = sum(1 for l in lines[1:] if l.split('\t')[1] == 'use' and l.split('\t')[3])
    print(f"\n# Total entries: {sum(counts.values())}. "
          f"use: {total_use} ({with_methods} with methods, {total_use - with_methods} empty). "
          f"impl: {counts.get('impl',0)}, test: {counts.get('test',0)}, "
          f"gen: {counts.get('gen',0)}, FP: {counts.get('FP',0)}",
          file=sys.stderr)


if __name__ == '__main__':
    main()
