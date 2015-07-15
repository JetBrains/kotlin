package test;

import java.lang.annotation.*;

public class AnnotationTargets {

    public @interface base {

    }

    @Target(ElementType.ANNOTATION_TYPE)
    public @interface annotation {

    }

    @Target(ElementType.CONSTRUCTOR)
    public @interface constructor {

    }

    @Target(ElementType.FIELD)
    public @interface field {

    }


    @Target(ElementType.LOCAL_VARIABLE)
    public @interface local {

    }

    @Target(ElementType.METHOD)
    public @interface method {

    }

    @Target(ElementType.PACKAGE)
    public @interface packag {

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