#!/bin/sh

set -e

OLD=<old>
NEW=<new>

for p in `find $OLD -type d -name '*-callsites' | cut -c79-`; do
  diff -r $OLD/$p $NEW/$p
  if [[ "$?" -ne "0" ]]; then
    echo "[diff] $p"
    echo
  fi
done

for p in `find $NEW -type d -name '*-callsites' | cut -c87-`; do
  diff -r $OLD/$p $NEW/$p
  if [[ "$?" -ne "0" ]]; then
    echo "[diff] $p"
    echo
  fi
done
