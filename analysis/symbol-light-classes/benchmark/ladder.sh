#!/bin/bash
# usage: ladder.sh <series> <rounds> <name=commit>...
# Runs the benchmark for every state in order, <rounds> times (state order rotates per round); labels <series>-<name>-<i>.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
series=$1; rounds=$2; shift 2
states=("$@")
cd "$T/../../.." || exit 1
paths=(analysis/symbol-light-classes/src analysis/light-classes-base/src)
n=${#states[@]}
for i in $(seq 1 $rounds); do
  for k in $(seq 0 $((n - 1))); do
    st=${states[$(( (k + i - 1) % n ))]}
    name=${st%%=*}; commit=${st#*=}
    git checkout -q $commit -- "${paths[@]}" || exit 1
    $T/slcbench.sh $series-$name-$i >> $S/bench/ladder-$series.log 2>&1
  done
done
git checkout -q HEAD -- "${paths[@]}"
