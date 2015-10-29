package test

import kotlin.reflect.KClass

inline fun <reified T : Any> injectFnc(): KClass<T> = {
    T::class
} ()

public class Box

