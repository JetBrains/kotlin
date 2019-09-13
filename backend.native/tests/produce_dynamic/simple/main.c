#include <stdio.h>
#include "testlib_api.h"

#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x
#define CAST(T, v) testlib_kref_ ## T { .pinned = v }

void errorHandler(const char* str) {
  printf("Error handler: %s\n", str);
}

int main(void) {
    T_(Singleton) singleton = __ kotlin.root.Singleton._instance();
    T_(Base) base = __ kotlin.root.Base.Base();
    T_(Child) child = __ kotlin.root.Child.Child();
    T_(Impl1) impl1 = __ kotlin.root.Impl1.Impl1();
    T_(Impl2) impl2 = __ kotlin.root.Impl2.Impl2();
    T_(Base) casted_child = { .pinned = child.pinned };
    T_(I) casted_impl1 = { .pinned = impl1.pinned };
    T_(I) casted_impl2 = { .pinned = impl2.pinned };
    T_(Enum) enum1 = __ kotlin.root.Enum.HUNDRED.get();
    T_(Codeable) object1 = __ kotlin.root.get_an_object();
    T_(Data) data = __ kotlin.root.getMutable();
    T_(kotlin_Int) nullableInt = __ createNullableInt(77);
    T_(kotlin_Unit) nullableUnit = __ createNullableUnit();
    T_(kotlin_Int) nullableIntNull = { .pinned = 0 };
    T_(kotlin_Unit) nullableUnitNull = { .pinned = 0 };
    T_(EnumWithInterface) enum2 = __ kotlin.root.EnumWithInterface.ZERO.get();

    const char* string1 = __ kotlin.root.getString();
    const char* string2 = __ kotlin.root.Singleton.toString(singleton);
    const char* string3 = __ kotlin.root.Data.get_string(data);
    const char* string4 = __ kotlin.root.getNullableString(0);
    const char* string5 = __ kotlin.root.getNullableString(1);

    __ kotlin.root.hello();
    __ kotlin.root.Base.foo(base);
    __ kotlin.root.Base.fooParam(base, "a", 1, "q");
    __ kotlin.root.Child.fooParam(child, "b", 2, (char*)0);
    __ kotlin.root.Base.fooParam(casted_child, "c", 3, (char*)0);
    __ kotlin.root.I.foo(casted_impl1, "d", 4, casted_impl1);
    __ kotlin.root.I.foo(casted_impl2, "e", 5, casted_impl2);

    printf("String is %s nullable is %s null is %s\n", string1, string4,
        string5 ? "BAD" : "OK");

    printf("RO property is %d\n", __ kotlin.root.Child.get_roProperty(child));
     __ kotlin.root.Child.set_rwProperty(child, 238);
    printf("RW property is %d\n", __ kotlin.root.Child.get_rwProperty(child));

    printf("enum100 = %d\n",  __ kotlin.root.Enum.get_code(enum1));
    printf("enum42 = %d\n",  __ kotlin.root.EnumWithInterface.foo(enum2));

    printf("object = %d\n",  __ kotlin.root.Codeable.asCode(object1));

    printf("singleton = %s\n",  string2);

    printf("mutable = %s\n",  string3);

    topLevelFunctionVoidFromC(42, nullableInt, nullableUnit, 0);
    __ kotlin.root.topLevelFunctionVoid(42, nullableInt, nullableUnit, 0);
    printf("topLevel = %d %d\n", topLevelFunctionFromC(780, 3), __ kotlin.root.topLevelFunctionFromCShort(5, 2));

    __ kotlin.root.useInlineClasses(42, "bar", base);

    __ kotlin.root.testNullableWithNulls(nullableIntNull, nullableUnitNull);

    __ DisposeStablePointer(singleton.pinned);
    __ DisposeString(string1);
    __ DisposeString(string2);
    __ DisposeString(string3);
    __ DisposeString(string4);
    __ DisposeString(string5);
    __ DisposeStablePointer(base.pinned);
    __ DisposeStablePointer(child.pinned);
    __ DisposeStablePointer(impl1.pinned);
    __ DisposeStablePointer(impl2.pinned);
    __ DisposeStablePointer(enum1.pinned);
    __ DisposeStablePointer(object1.pinned);
    __ DisposeStablePointer(nullableInt.pinned);
    __ DisposeStablePointer(enum2.pinned);

    __ kotlin.root.setCErrorHandler(&errorHandler);
    __ kotlin.root.throwException();

    return 0;
}

