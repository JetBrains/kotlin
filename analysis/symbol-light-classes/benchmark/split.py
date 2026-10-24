"""Splits a test data file with `// FILE: <path>` sections into a directory tree.

usage: split.py <file.kt> <out-dir>
"""
import os, sys
src, out = sys.argv[1], sys.argv[2]
path, lines = None, []
def flush():
    if path is None: return
    os.makedirs(os.path.dirname(os.path.join(out, path)), exist_ok=True)
    with open(os.path.join(out, path), 'w') as f: f.write(''.join(lines))
for line in open(src):
    if line.startswith('// FILE: '):
        flush(); path, lines = line[len('// FILE: '):].strip(), []
    else:
        lines.append(line)
flush()
