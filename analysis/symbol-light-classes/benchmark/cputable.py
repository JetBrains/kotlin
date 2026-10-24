"""Summarizes cpu.txt lines "<series>-<suite>-<A|B><i> cpu=.. wall=.. gc=.." per suite: median CPU and wall of A and B.

usage: cputable.py <cpu.txt> <series>
"""
import sys, re, collections, statistics
path, series = sys.argv[1], sys.argv[2]
d = collections.defaultdict(lambda: collections.defaultdict(list))
for line in open(path):
    m = re.match(rf'{re.escape(series)}-(\w+)-([AB])\d+ cpu=([\d.]+) wall=([\d.]+)', line)
    if m: d[m.group(1)][m.group(2)].append((float(m.group(3)), float(m.group(4))))
print(f"{'suite':8} {'A cpu s':>8} {'B cpu s':>8} {'diff':>7} {'A wall s':>9} {'B wall s':>9} {'diff':>7}  n")
for suite, st in d.items():
    a, b = st['A'], st['B']
    if not a or not b: continue
    ac, bc = statistics.median(x[0] for x in a), statistics.median(x[0] for x in b)
    aw, bw = statistics.median(x[1] for x in a), statistics.median(x[1] for x in b)
    print(f"{suite:8} {ac:8.1f} {bc:8.1f} {100*(bc-ac)/ac:+6.1f}% {aw:9.1f} {bw:9.1f} {100*(bw-aw)/aw:+6.1f}%  {len(a)},{len(b)}")
