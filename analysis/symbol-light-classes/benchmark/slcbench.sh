#!/bin/bash
# usage: slcbench.sh <label> [extra -D options separated by '|'] [jfr]
# Runs SymbolLightClassesBenchmark once (prebuilt first); results go to $S/bench/slc-bench.tsv, the log to $S/bench/<label>.log.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
label=$1; extra=${2:-}; mode=${3:-}
cd "$T/../../.." || exit 1
mkdir -p analysis/symbol-light-classes/build/jfr
./gradlew --init-script $T/prof.init.gradle.kts -Pprof.prebuild=true :analysis:symbol-light-classes:test -q > $S/bench/prebuild-$label.log 2>&1 \
    || { echo "prebuild of $label failed"; tail -30 $S/bench/prebuild-$label.log; exit 1; }
jvmargs="-Dslc.bench.label=$label|-javaagent:$S/cpu-agent.jar=$S/bench/cpu.txt;$label"
[ -n "$extra" ] && jvmargs="$jvmargs|$extra"
jfrargs=()
if [ "$mode" = jfr ]; then
  mkdir -p $S/jfr/$label; rm -f $S/jfr/$label/*.jfr $S/jfr/$label.cpu.collapsed $S/jfr/$label.alloc.collapsed
  jfrargs=(-Pprof.jdk=17 "-Pprof.jfr.dir=$S/jfr/$label" "-Pprof.jfr.settings=$T/profile-1ms.jfc")
fi
./gradlew --init-script $T/prof.init.gradle.kts "-Pprof.out.dir=$S/bench" "-Pprof.jvmargs=$jvmargs" "${jfrargs[@]}" \
    :analysis:symbol-light-classes:test --tests 'org.jetbrains.kotlin.light.classes.symbol.SymbolLightClassesBenchmark' --rerun -i > $S/bench/$label.log 2>&1
rc=$?
echo "=== $label rc=$rc"
grep -E 'SLC benchmark' $S/bench/$label.log | sed 's/^.*SLC benchmark/SLC benchmark/'
grep "^$label " $S/bench/cpu.txt | tail -1
if [ "$mode" = jfr ]; then
  for f in $S/jfr/$label/*.jfr; do java -Xmx12g $T/JfrAgg.java "$f" "$S/jfr/$label" 'Test worker|ForkJoinPool'; done
fi
