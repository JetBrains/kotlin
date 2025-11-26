// RUN_PIPELINE_TILL: BACKEND
// FILE: dao.kt
package my.project.dao

data class User(val name: String)

// FILE: domain.kt
package my.project.domain

data class User(val fullName: String)

// FILE: conversion.kt
package my.project.conversion

import package my.project.dao as marshal
import package my.project.domain
import package my.project as p

fun domain.User.toDao(): marshal.User = marshal.User(name = fullName)
fun p.dao.User.toDomain(): p.domain.User = domain.User(fullName = name)

/* GENERATED_FIR_TAGS: classDeclaration, data, funWithExtensionReceiver, functionDeclaration, primaryConstructor,
propertyDeclaration */
