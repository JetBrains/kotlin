"""Shares of stacks that pass through SLC production code, and what they spend time in.

usage: slcshare.py <collapsed> [top]
"""
import sys, re, collections
path = sys.argv[1]; top = int(sys.argv[2]) if len(sys.argv) > 2 else 40
PROD = re.compile(r'^org\.jetbrains\.kotlin\.(light\.classes\.symbol\.(?!base\.|source\.|decompiled\.|TestGenerator)|asJava\.(?!PsiClassRenderer|LightClassTestCommon))')
st = []
for line in open(path):
    i = line.rfind(' ')
    st.append((line[:i].split(';'), int(line[i + 1:])))
total = sum(n for _, n in st)
hit = 0
entry = collections.Counter()   # outermost SLC frame
leafpkg = collections.Counter()
for fr, n in st:
    idx = [i for i, m in enumerate(fr) if PROD.search(m)]
    if not idx: continue
    hit += n
    entry[fr[idx[0]]] += n
print(f'total {total}, under SLC production code {hit} ({100*hit/total:.1f}%)')
print('--- outermost SLC frames')
for k, v in entry.most_common(top): print(f'{v:8d} {100*v/hit:5.1f}% {k}')
