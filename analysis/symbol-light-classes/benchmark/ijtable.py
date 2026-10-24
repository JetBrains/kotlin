"""Summarizes ijbench.tsv for an A/B series (labels <series>-A<i>, <series>-B<i>).

usage: ijtable.py <tsv> <series> [first-round] [state-a] [state-b]
For each phase: median over the runs and the rounds >= first-round (default 3; `cold` has round 0 only)
of thread CPU, process CPU, wall time and allocation; the difference of B against A.
"""
import sys, collections, statistics
tsv, series = sys.argv[1], sys.argv[2]
first = int(sys.argv[3]) if len(sys.argv) > 3 else 3
sa, sb = (sys.argv[4], sys.argv[5]) if len(sys.argv) > 5 else ('A', 'B')
d = collections.defaultdict(lambda: collections.defaultdict(list))
for line in open(tsv):
    label, phase, rnd, cpu, alloc, pcpu, wall = line.rstrip('\n').split('\t')
    if not label.startswith(series + '-'): continue
    st = label[len(series) + 1]
    if phase != 'cold' and int(rnd) < first: continue
    d[st][phase].append((int(cpu) / 1e6, int(pcpu) / 1e6, int(wall) / 1e6, int(alloc) / 1e6))
names = ['thread cpu ms', 'process cpu ms', 'wall ms', 'alloc MB']
print(f"{'phase':20}" + ''.join(f"{n:>26}" for n in names) + '   n(A,B)')
for p in ['cold', 'java_edit', 'kotlin_in_block', 'kotlin_out_of_block']:
    a, b = d[sa][p], d[sb][p]
    if not a or not b: continue
    row = f"{p:20}"
    for i in range(4):
        ma = statistics.median(x[i] for x in a); mb = statistics.median(x[i] for x in b)
        row += f"{ma:9.0f} {mb:7.0f} {100*(mb-ma)/ma:+7.1f}%"
    print(row + f"   {len(a)},{len(b)}")
