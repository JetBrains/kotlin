// TARGET_BACKEND: NATIVE
// MODULE: cinterop_kt63048
// FILE: kt63048.def
language = Objective-C
headers = kt63048.h

// FILE: kt63048.h
#import "Foundation/NSString.h"
#import "Foundation/NSObject.h"

@interface WithClassProperty : NSObject
-(instancetype) init;
@property (class, readonly, copy) NSString * stringProperty;
@end


// FILE: kt63048.m
#import "kt63048.h"

@implementation WithClassProperty : NSObject
-(instancetype) init {}
+ (NSString *) stringProperty { return @"153"; }
@end


// MODULE: main(cinterop_kt63048)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import kt63048.*
import kotlin.test.assertEquals

@ObjCName("KotlinImplWithCompanionPropertyOverride")
class Impl : WithClassProperty() {
    companion object : WithClassPropertyMeta() {
        override fun stringProperty(): String? = "42"
    }
}

@ObjCName("KotlinImplWithoutCompanionPropertyOverride")
class ImplWithoutOverride : WithClassProperty() {
    companion object : WithClassPropertyMeta() {
    }
}

fun box(): String {
    assertEquals("42", Impl.stringProperty())
    assertEquals("153", ImplWithoutOverride.stringProperty())

    return "OK"
}