"""Median CPU and allocation per phase for every state of a ladder series (labels <series>-<name>-<i>).

usage: laddertable.py <tsv> <series> <name>...
"""
import sys, collections, statistics
tsv, series, names = sys.argv[1], sys.argv[2], sys.argv[3:]
d = collections.defaultdict(lambda: collections.defaultdict(list))
for line in open(tsv):
    label, _, phase, _, cpu, alloc = line.rstrip('\n').split('\t')
    if not label.startswith(series + '-'): continue
    name = label[len(series) + 1:].rsplit('-', 1)[0]
    d[phase][name].append((int(cpu) / 1e6, int(alloc) / 1e6))
phases = ['render_cold', 'render_fresh', 'render_warm', 'resolve_fresh', 'resolve_warm', 'lookup_warm', 'finder_warm', 'list_cold']
print('phase,metric,' + ','.join(names))
for p in phases:
    if p not in d: continue
    for m, idx in (('cpu_ms', 0), ('alloc_mb', 1)):
        vals = [statistics.median(x[idx] for x in d[p][n]) if d[p][n] else float('nan') for n in names]
        print(f'{p},{m},' + ','.join(f'{v:.1f}' for v in vals))
