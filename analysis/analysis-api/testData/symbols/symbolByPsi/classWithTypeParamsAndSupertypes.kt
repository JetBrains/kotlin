// WITH_STDLIB

import org.jetbrains.annotations.NotNull

abstract class X<T, U>

abstract class Y<T> : X<String, T>

class A<@NotNull T, R> : Y<Pair<T, R>>
