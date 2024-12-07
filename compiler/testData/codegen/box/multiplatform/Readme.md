# MPP box tests

## K1 and K2

K1 and K2 MPP tests use incompatible test modules structure:
- K1 doesn't fully support `// MODULE: platform()()(common)` structure, and so
  most of the K1 tests are written in the same module
- K2 doesn't support `expect`s and `actual`s in the same module and requires proper module structure

Because of this difference, almost any MPP test will fail either in K1 or K2, so it was decided to completely split the MPP testdata
  to K1-specific (`k1` directory) and K2-specific (`k2` directory). For tests in `k1` directory K2-based tests are not generated and
  vice versa.

## Directives layout

Usually MPP tests include more test directives compared to regular codegen tests, so the following test header pattern is proposed
  to keep tests consistent and readable:

```kotlin
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: ...
// IGNORE_BACKEND: ...
// ISSUE: ...
// other directives

// MODULE: ...
```

## Test templates

Here are some templates for common MPP testing scenarios

### Simple two-level project

```kotlin
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: ...
// IGNORE_BACKEND: ...
// ISSUE: KT-...

// MODULE: common
// FILE: common.kt

// MODULE: platform()()(common)
// FILE: platform.kt
```

### Simple three-level project

```kotlin
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: ...
// IGNORE_BACKEND: ...
// ISSUE: KT-...

// MODULE: common
// FILE: common.kt

// MODULE: intermediate()()(common)
// FILE: intermediate.kt

// MODULE: platform()()(intermediate)
// FILE: platform.kt
```

### Two-level project with MPP dependency

```kotlin
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: ...
// IGNORE_BACKEND: ...
// ISSUE: KT-...

// MODULE: common-lib
// FILE: common.kt

// MODULE: platform-lib()()(common-lib)
// FILE: platform.kt

// MODULE: common-app(common-lib)
// FILE: common.kt

// MODULE: platform-app(platform-lib)()(common-app)
// FILE: platform.kt
```
