#!/bin/sh

set -e

OLD=<old>
NEW=<new>

echo '[old] number of kexe files'
  find $OLD -type f -name '*.kexe' | grep -v '\.kexe\.' | wc -l
echo '[new] number of kexe files'
  find $NEW -type f -name '*.kexe' | grep -v '\.kexe\.' | wc -l

echo
echo '[old] number of kexe log files'
  find $OLD -type f -name '*.kexe.log' | wc -l
echo '[new] number of kexe log files'
  find $NEW -type f -name '*.kexe.log' | wc -l

echo
echo '[old] number of klib log files'
  find $OLD -type f -name '*.klib.log' | wc -l
echo '[new] number of klib log files'
  find $NEW -type f -name '*.klib.log' | wc -l

echo
echo '[old] callsite directories'
  find $OLD -type d -name '*-callsites' | wc -l
echo '[new] callsite directories'
  find $NEW -type d -name '*-callsites' | wc -l

echo
echo '[old] number of klib log files compiled with hacked compiler and having kotlin version'
  find $OLD -type d -name '*-callsites' | awk 'BEGIN{FS="-callsites"}{print $1".log"}' | xargs grep -h "Kotlin version:" | wc -l
echo '[new] number of klib log files compiled with hacked compiler and having kotlin version'
  find $NEW -type d -name '*-callsites' | awk 'BEGIN{FS="-callsites"}{print $1".log"}' | xargs grep -h "Kotlin version:" | wc -l

echo
echo '[old] unique kotlin versions in klib log files compiled with hacked compiler'
  find $OLD -type d -name '*-callsites' | awk 'BEGIN{FS="-callsites"}{print $1".log"}' | xargs grep -h "Kotlin version:" | sort -u
echo '[new] unique kotlin versions in klib log files compiled with hacked compiler'
  find $NEW -type d -name '*-callsites' | awk 'BEGIN{FS="-callsites"}{print $1".log"}' | xargs grep -h "Kotlin version:" | sort -u

echo
echo '[old] kotlin versions stats in klib log files'
  find $OLD -type f -name '*.klib.log' -exec grep 'Kotlin version:' {} \; | awk 'BEGIN{FS=":"}{print $2}' | sort | uniq -c
echo '[new] kotlin versions stats in klib log files'
  find $NEW -type f -name '*.klib.log' -exec grep 'Kotlin version:' {} \; | awk 'BEGIN{FS=":"}{print $2}' | sort | uniq -c




