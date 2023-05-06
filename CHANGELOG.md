Change Log
==========

## Version 0.13.0

_2023-05-06_

**New**
* Support Kotlin 1.8.20 (#89).

**Fixes**
* Correctly align `in` operator. (#96).

## Version 0.12.2

_2023-04-09_

**Fixes**
* Correctly align `in` operator. (#95).

## Version 0.12.1

_2023-04-08_

**New**
* Support receivers of infix functions (#69).

**Fixes**
* Ignore implicit varargs array (#84).
* Ignore body of object literals (#85).

**Other**
* Run tests in parallel (#73) - @christophsturm.
* Upgrade to Gradle 8 (#87) - @TWiStErRob.
* Add Kotlinter for code styling (#86) - @christophsturm.

## Version 0.12.0

_2022-07-10_

**New**
* Support Kotlin 1.7.0 (#67).
* Allow excluding Gradle source sets from transformation (#55).

**Fixes**
* Ignore smart casting from diagram (#60).
* Properly align `is` operator result (#59).
* Support JVM static functions for transformation (#52).

## Version 0.11.0

_2021-11-18_

**New**
* Support Kotlin 1.6.0 (#50).

## Version 0.10.0

_2021-06-29_

**New**
* Support Kotlin 1.5.20.

## Version 0.9.0

_2021-06-02_

**New**
* Support Kotlin 1.5.10.

## Version 0.8.1

_2021-05-12_

**Fixes**
* Fix diagramming of expressions which contain lambdas (#44)

## Version 0.8.0

_2021-05-06_

**New**
* Compile against Kotlin 1.5.0 (#40).
* Support generic parameter diagramming (#39).
  * Added support for `assertFalse` style functions.
  * Added support for non-boolean functions.
* Support multiple parameter functions (#41).
  * Added support for `assertEquals` style functions.
  * Function signature must still end with a `String` or `() -> String` accepting parameter.

## Version 0.7.0

_2021-02-04_

**New**
* Support Kotlin 1.4.30.

**Fixes**
* Regex `matches` function formats poorly and exception (#31).

## Version 0.6.1

_2020-11-29_

Fixes:
 * Better support for arithmetic in assertion condition (#27).

## Version 0.6.0

_2020-11-21_

Changes:
 * Support Kotlin 1.4.20.

## Version 0.5.3

_2020-09-28_

Bug Fixes:
 * Fix member and extension function transformation.
 * Fix generation of message lambda for non-inline functions.

## Version 0.5.2

_2020-09-25_

Bug Fixes:
 * Skip transformation if entire expression is constant (`true` or `false`)
   (#23).
 * Fix inlining of lambda parameter to transformed function call on Kotlin/JS
   and Kotlin/Native (#22).

## Version 0.5.1

_2020-09-14_

Bug Fixes:
 * Support Windows-style line-separators in compiled files (#20).

## Version 0.5.0

_2020-08-29_

 * Support for Kotlin/Native (#18).

## Version 0.4.0

_2020-08-21_

 * Support Kotlin 1.4.0.
 
## Version 0.3.1

_2020-05-25_

 * Fix: Do not include wrapper class for top-level function in Gradle plugin
   default function (#13).

## Version 0.3.0

_2020-03-07_

 * Support Kotlin 1.3.70.
 * Fix: Including Kotlin wrapper class for top-level functions in Gradle
   configuration is no longer required.

## Version 0.2.0

_2020-02-12_

 * Support configuring of which functions are transformed (eg, assert, require,
   check, assertTrue). This works for any function which takes a Boolean and a
   String. 
 * Support Boolean expressions which are split onto multiple lines.

## Version 0.1.0

_2020-02-10_

 * Initial release
