// FILE: test/AnnotationTargets.java

package test;

import java.lang.annotation.*;

public class AnnotationTargets {

    public @interface base {

    }

    @Target(ElementType.ANNOTATION_TYPE)
    public @interface meta {

    }

    @Target(ElementType.CONSTRUCTOR)
    public @interface konstructor {

    }

    @Target(ElementType.FIELD)
    public @interface fieldann {

    }


    @Target(ElementType.LOCAL_VARIABLE)
    public @interface local {

    }

    @Target(ElementType.METHOD)
    public @interface method {

    }

    @Target(ElementType.PARAMETER)
    public @interface parameter {

    }


    @Target(ElementType.TYPE)
    public @interface type {

    }

    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
    public @interface multiple {

    }
}

// FILE: test/AnnotationTargets.kt

@file:AnnotationTargets.type
package test

import test.AnnotationTargets.*

@base @meta @type @konstructor annotation class KMeta

@base @meta @type @method @multiple class KClass(
        @base @fieldann @parameter val y:
        @base @type Int) {

    @base @multiple @fieldann @local val x = 0
    @method @konstructor @type get

    @base @method @multiple @konstructor
    fun foo(@parameter @type i:
    @base @multiple Int
    ): @fieldann @parameter Int {

        @local @base @multiple @fieldann val j = i + 1
        @base @multiple return j
    }

    @base @method @konstructor constructor(): this(0)
}
