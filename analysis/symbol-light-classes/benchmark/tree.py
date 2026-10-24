"""Prints the call tree below the outermost frame matching <root-regex>, merged over all stacks.

usage: tree.py <collapsed> <root-regex> [depth] [min-share-%] [skip-regex]
Frames matching skip-regex are elided (their children are attached to the parent) to compress wrappers.
"""
import sys, re, collections
path, root = sys.argv[1], re.compile(sys.argv[2])
depth = int(sys.argv[3]) if len(sys.argv) > 3 else 12
minshare = float(sys.argv[4]) if len(sys.argv) > 4 else 1.0
skip = re.compile(sys.argv[5]) if len(sys.argv) > 5 else None
def node(): return [0, {}]
tree = node(); total = 0
for line in open(path):
    i = line.rfind(' '); fr = line[:i].split(';'); n = int(line[i + 1:])
    idx = [k for k, m in enumerate(fr) if root.search(m)]
    if not idx: continue
    total += n
    cur = tree; cur[0] += n
    for m in fr[idx[0]:idx[0] + 400]:
        if skip and skip.search(m) and not root.search(m): continue
        cur = cur[1].setdefault(m, node()); cur[0] += n
def short(m):
    p = m.split('.')
    return '.'.join(p[-2:]) if len(p) > 1 else m
def pr(name, nd, d):
    if d > depth: return
    share = 100 * nd[0] / max(total, 1)
    if share < minshare: return
    print(f"{'  ' * d}{nd[0]:6d} {share:5.1f}% {short(name)}")
    for k, v in sorted(nd[1].items(), key=lambda kv: -kv[1][0]):
        pr(k, v, d + 1)
print('total', total)
for k, v in sorted(tree[1].items(), key=lambda kv: -kv[1][0]): pr(k, v, 0)
