"""Analyses of collapsed stacks ("root;...;leaf count").

usage:
  collapsed.py incl <file> [top] [frame-filter-regex]        inclusive share of frames
  collapsed.py self <file> [top] [within-regex]              self share (optionally only stacks containing within-regex)
  collapsed.py under <file> <regex> [top]                    inclusive share of callees under the outermost match
  collapsed.py callers <file> <regex> [top] [filter]         inclusive share of callers above the innermost match
  collapsed.py buckets <file> <buckets.tsv> [within-regex]   first matching bucket (priority order) per stack
  collapsed.py paths <file> <regex> <filter> [top] [depth]   common caller chains above the innermost match
"""
import sys, re, collections


def load(path):
    out = []
    with open(path) as f:
        for line in f:
            line = line.rstrip('\n')
            i = line.rfind(' ')
            if i < 0: continue
            out.append((line[:i].split(';'), int(line[i + 1:])))
    return out


def short(m):
    return '.'.join(m.split('.')[-2:])


cmd, path = sys.argv[1], sys.argv[2]
st = load(path)
total = sum(c for _, c in st)

if cmd == 'incl':
    top = int(sys.argv[3]) if len(sys.argv) > 3 else 50
    fre = re.compile(sys.argv[4]) if len(sys.argv) > 4 else None
    c = collections.Counter()
    for fr, n in st:
        for m in set(fr):
            if fre is None or fre.search(m): c[m] += n
    print('total', total)
    for k, v in c.most_common(top): print(f'{v:8d} {100 * v / total:5.1f}% {k}')

elif cmd == 'self':
    top = int(sys.argv[3]) if len(sys.argv) > 3 else 50
    within = re.compile(sys.argv[4]) if len(sys.argv) > 4 else None
    c = collections.Counter(); hit = 0
    for fr, n in st:
        if within and not any(within.search(m) for m in fr): continue
        hit += n; c[fr[-1]] += n
    print('total', total, 'matching', hit)
    for k, v in c.most_common(top): print(f'{v:8d} {100 * v / total:5.1f}% {100 * v / max(hit, 1):5.1f}%sub {k}')

elif cmd == 'under':
    rx = re.compile(sys.argv[3]); top = int(sys.argv[4]) if len(sys.argv) > 4 else 50
    c = collections.Counter(); hit = 0
    for fr, n in st:
        idx = [i for i, m in enumerate(fr) if rx.search(m)]
        if not idx: continue
        hit += n
        for m in set(fr[idx[0] + 1:]): c[m] += n
    print('total', total, 'matching', hit, f'{100 * hit / total:.1f}%')
    for k, v in c.most_common(top): print(f'{v:8d} {100 * v / total:5.1f}% {100 * v / max(hit, 1):5.1f}%sub {k}')

elif cmd == 'callers':
    rx = re.compile(sys.argv[3]); top = int(sys.argv[4]) if len(sys.argv) > 4 else 50
    ff = re.compile(sys.argv[5]) if len(sys.argv) > 5 else None
    c = collections.Counter(); hit = 0
    for fr, n in st:
        idx = [i for i, m in enumerate(fr) if rx.search(m)]
        if not idx: continue
        hit += n
        for m in set(fr[:idx[-1]]):
            if ff is None or ff.search(m): c[m] += n
    print('total', total, 'matching', hit, f'{100 * hit / total:.1f}%')
    for k, v in c.most_common(top): print(f'{v:8d} {100 * v / total:5.1f}% {100 * v / max(hit, 1):5.1f}%sub {k}')

elif cmd == 'buckets':
    bs = []
    for line in open(sys.argv[3]):
        line = line.rstrip('\n')
        if not line or line.startswith('#'): continue
        name, r = line.split('\t', 1); bs.append((name, re.compile(r)))
    within = re.compile(sys.argv[4]) if len(sys.argv) > 4 else None
    c = collections.Counter(); hit = 0
    for fr, n in st:
        if within and not any(within.search(m) for m in fr): continue
        hit += n
        for name, r in bs:
            if any(r.search(m) for m in fr): c[name] += n; break
        else: c['other'] += n
    print('total', total, 'considered', hit)
    for name, _ in bs + [('other', None)]:
        print(f'{c[name]:8d} {100 * c[name] / max(hit, 1):5.1f}% {name}')

elif cmd == 'paths':
    rx = re.compile(sys.argv[3]); ff = re.compile(sys.argv[4])
    top = int(sys.argv[5]) if len(sys.argv) > 5 else 30
    depth = int(sys.argv[6]) if len(sys.argv) > 6 else 8
    c = collections.Counter(); hit = 0
    for fr, n in st:
        idx = [i for i, m in enumerate(fr) if rx.search(m)]
        if not idx: continue
        hit += n; chain = []
        for m in reversed(fr[:idx[-1]]):
            if ff.search(m):
                s = short(m)
                if not chain or chain[-1] != s: chain.append(s)
            if len(chain) >= depth: break
        c[' <- '.join(chain)] += n
    print('matching', hit, f'{100 * hit / total:.1f}%')
    for k, v in c.most_common(top): print(f'{v:7d} {100 * v / total:5.1f}% {k}')
