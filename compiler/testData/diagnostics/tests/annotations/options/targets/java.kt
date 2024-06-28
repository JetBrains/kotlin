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

@base @meta @type <!WRONG_ANNOTATION_TARGET!>@konstructor<!> annotation class KMeta

@base <!WRONG_ANNOTATION_TARGET!>@meta<!> @type <!WRONG_ANNOTATION_TARGET!>@method<!> <!WRONG_ANNOTATION_TARGET!>@multiple<!> class KClass(
        @base @fieldann @parameter val y:
        <!WRONG_ANNOTATION_TARGET!>@base<!> <!WRONG_ANNOTATION_TARGET!>@type<!> Int) {

    @base @multiple @fieldann <!WRONG_ANNOTATION_TARGET!>@local<!> val x = 0
    @method <!WRONG_ANNOTATION_TARGET!>@konstructor<!> <!WRONG_ANNOTATION_TARGET!>@type<!> get

    @base @method @multiple <!WRONG_ANNOTATION_TARGET!>@konstructor<!>
    fun foo(@parameter <!WRONG_ANNOTATION_TARGET!>@type<!> i:
    <!WRONG_ANNOTATION_TARGET!>@base<!> <!WRONG_ANNOTATION_TARGET!>@multiple<!> Int
    ): <!WRONG_ANNOTATION_TARGET!>@fieldann<!> <!WRONG_ANNOTATION_TARGET!>@parameter<!> Int {

        @local @base <!WRONG_ANNOTATION_TARGET!>@multiple<!> <!WRONG_ANNOTATION_TARGET!>@fieldann<!> val j = i + 1
        <!WRONG_ANNOTATION_TARGET!>@base<!> <!WRONG_ANNOTATION_TARGET!>@multiple<!> return j
    }

    @base <!WRONG_ANNOTATION_TARGET!>@method<!> @konstructor constructor(): this(0)
}
