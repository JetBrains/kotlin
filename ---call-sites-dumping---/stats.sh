#!/bin/sh

set -e

OLD=<old>

STATS=./stats.txt
rm -f $STATS
touch $STATS

for d in `find $OLD -type d -name '*-callsites'`; do
  find $d -type f -exec cat {} \; >> $STATS
done

mv $STATS $STATS.tmp
cat $STATS.tmp | sort > $STATS
rm $STATS.tmp

echo 'total call sites'
cat $STATS | wc -l

echo 'total call sites excl. stdlib & intrinsics'
cat $STATS | egrep -v '^kotlin' | wc -l

echo 'unique signatures at call sites'
cat $STATS | sort -u | wc -l

echo 'unique signatures at call sites excl. stdlib & intrinsics'
cat $STATS | egrep -v '^kotlin' | sort -u | wc -l
