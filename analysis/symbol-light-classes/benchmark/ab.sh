#!/bin/bash
# usage: ab.sh <series> <rounds> <stateA-commit> <stateB-commit> [extra -D options]
# Alternates A and B: restores analysis/symbol-light-classes/src and analysis/light-classes-base/src from the given commit,
# then runs the benchmark. Labels: <series>-A<i>, <series>-B<i>. Restores HEAD sources at the end.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
series=$1; rounds=$2; a=$3; b=$4; extra=${5:-}
cd "$T/../../.." || exit 1
paths=(analysis/symbol-light-classes/src analysis/light-classes-base/src)
for i in $(seq 1 $rounds); do
  for st in A B; do
    commit=$a; [ $st = B ] && commit=$b
    git checkout -q $commit -- "${paths[@]}" || exit 1
    $T/slcbench.sh $series-$st$i "$extra" | tee -a $S/bench/ab-$series.log
  done
done
git checkout -q HEAD -- "${paths[@]}"
git status --short
