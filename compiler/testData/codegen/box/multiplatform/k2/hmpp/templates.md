This file contains some template module structures for different setups

#### 2-2 modules
```kotlin
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

// MODULE: lib-platform()()(lib-common)

// MODULE: app-common(lib-common)

// MODULE: app-platform(lib-platform)()(app-common)
```

#### 3-3 modules
```kotlin
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

// MODULE: lib-inter()()(lib-common)

// MODULE: lib-platform()()(lib-inter)

// MODULE: app-common(lib-common)

// MODULE: app-inter(lib-inter)(lib-common)(app-common)

// MODULE: app-platform(lib-platform)()(app-inter)
```

#### 2-3 modules
```kotlin
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common

// MODULE: lib-platform()()(lib-common)

// MODULE: app-common(lib-common)

// MODULE: app-inter(lib-common)()(app-common)

// MODULE: app-platform(lib-platform)()(app-inter)
```

#### 3-4 (diamond) modules
```kotlin
// MODULE: lib-common

// MODULE: lib-inter()()(lib-common)

// MODULE: lib-platform()()(lib-inter)

// MODULE: app-common(lib-common)

// MODULE: app-inter1(lib-common)()(app-common)

// MODULE: app-inter2(lib-common)()(app-common)

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
```

#### 4-4 (diamond) modules
```kotlin
// MODULE: lib-common

// MODULE: lib-inter1()()(lib-common)

// MODULE: lib-inter2()()(lib-common)

// MODULE: lib-platform()()(lib-inter1, lib-inter2)

// MODULE: app-common(lib-common)

// MODULE: app-inter1(lib-inter1)()(app-common)

// MODULE: app-inter2(lib-inter2)()(app-common)

// MODULE: app-platform(lib-platform)()(app-inter1, app-inter2)
```
