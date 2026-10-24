"""Buckets stacks under a regex by the innermost frame (nearest to the leaf) that matches any group.

usage: leafgroups.py <collapsed> <within-regex> <groups.tsv> [examples]
groups.tsv lines: <name>\t<regex>; for that innermost frame, the first group in file order whose regex matches wins.
"""
import sys, re, collections
path, within, gfile = sys.argv[1], re.compile(sys.argv[2]), sys.argv[3]
examples = int(sys.argv[4]) if len(sys.argv) > 4 else 0
groups = [(l.split('\t')[0], re.compile(l.rstrip('\n').split('\t')[1])) for l in open(gfile) if l.strip() and not l.startswith('#')]
c = collections.Counter(); hit = 0; ex = collections.defaultdict(collections.Counter)
for line in open(path):
    i = line.rfind(' '); fr = line[:i].split(';'); n = int(line[i + 1:])
    idx = [k for k, m in enumerate(fr) if within.search(m)]
    if not idx: continue
    sub = fr[idx[0]:]
    hit += n
    name = 'other'; frame = sub[-1]
    for m in reversed(sub):
        g = next((g for g, rx in groups if rx.search(m)), None)
        if g: name = g; frame = m; break
    c[name] += n; ex[name][frame] += n
print('matching', hit)
for k, v in c.most_common():
    print(f'{v:8d} {100*v/max(hit,1):5.1f}% {k}')
    for f, w in ex[k].most_common(examples): print(f'           {w:6d} {f}')
