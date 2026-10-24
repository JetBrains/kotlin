#!/bin/bash
# usage: jfr.sh <label> <gradle test args...>
# Prebuilds, then profiles the given test tasks with deep-stack JFR on JDK 17 (LL JFR events off) and aggregates the test threads
# into $S/jfr/<label>.cpu.collapsed and $S/jfr/<label>.alloc.collapsed.
T=$(cd "$(dirname "$0")" && pwd)
S=${SLC_BENCH_WORK:?set SLC_BENCH_WORK to a work directory, see README.md}
IJ=${INTELLIJ_HOME:-$HOME/code/intellij}
mkdir -p $S/bench $S/ij $S/jfr $S/ijjars
label=$1; shift
cd "$T/../../.." || exit 1
./gradlew --init-script $T/prof.init.gradle.kts -Pprof.prebuild=true "$@" -q --continue > $S/bench/prebuild-$label.log 2>&1 \
    || { echo "prebuild of $label failed"; tail -30 $S/bench/prebuild-$label.log; exit 1; }
mkdir -p $S/jfr/$label analysis/symbol-light-classes/build/jfr
rm -f $S/jfr/$label.cpu.collapsed $S/jfr/$label.alloc.collapsed
start=$(date +%s)
./gradlew --init-script $T/prof.init.gradle.kts -Pprof.jdk=17 "-Pprof.jfr.dir=$S/jfr/$label" \
    "-Pprof.jfr.settings=$T/profile-no-ll.jfc" \
    "-Pprof.jvmargs=-javaagent:$S/cpu-agent.jar=$S/bench/cpu-jfr.txt;$label:{project}" \
    "$@" -q --continue > "$S/bench/$label.log" 2>&1
rc=$?
echo "=== $label rc=$rc gradle_wall=$(( $(date +%s) - start ))s"
grep "^$label:" $S/bench/cpu-jfr.txt
ls -la $S/jfr/$label
for f in $S/jfr/$label/*.jfr; do
    java -Xmx12g $T/JfrAgg.java "$f" "$S/jfr/$label" 'ForkJoinPool-\d+-worker|Test worker'
done
