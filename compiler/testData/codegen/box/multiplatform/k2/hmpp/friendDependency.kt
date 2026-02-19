// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
internal val o = "O"

// MODULE: lib-platform()()(lib-common)
internal val k = "K"

// MODULE: app-common()(lib-common)()
val myO = o

// MODULE: app-platform()(lib-platform)(app-common)
fun box(): String {
    return myO + k
}