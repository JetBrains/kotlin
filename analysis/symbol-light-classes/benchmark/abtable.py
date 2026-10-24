"""Summarizes slc-bench.tsv for an A/B series: median of per-round CPU and allocation per phase, per state.

usage: abtable2.py <tsv> <series>
"""
import sys, collections, statistics
tsv, series = sys.argv[1], sys.argv[2]
data = collections.defaultdict(lambda: collections.defaultdict(list))  # state -> phase -> [(cpu, alloc)]
for line in open(tsv):
    label, name, phase, rnd, cpu, alloc = line.rstrip('\n').split('\t')
    if not label.startswith(series + '-'): continue
    state = label[len(series) + 1]
    data[state][phase].append((int(cpu) / 1e6, int(alloc) / 1e6))
phases = sorted({p for st in data.values() for p in st}, key=lambda p: ['render_cold','render_fresh','render_warm','resolve_fresh','resolve_warm','lookup_warm','finder_warm','list_cold'].index(p) if p in ['render_cold','render_fresh','render_warm','resolve_fresh','resolve_warm','lookup_warm','finder_warm','list_cold'] else 99)
print(f"{'phase':15} {'A cpu ms':>9} {'B cpu ms':>9} {'diff':>7} {'A MB':>8} {'B MB':>8} {'diff':>7}  n(A,B)")
for p in phases:
    a = data['A'][p]; b = data['B'][p]
    if not a or not b: continue
    ac = statistics.median(x[0] for x in a); bc = statistics.median(x[0] for x in b)
    am = statistics.median(x[1] for x in a); bm = statistics.median(x[1] for x in b)
    print(f"{p:15} {ac:9.1f} {bc:9.1f} {100*(bc-ac)/ac:+6.1f}% {am:8.1f} {bm:8.1f} {100*(bm-am)/am:+6.1f}%  {len(a)},{len(b)}")
