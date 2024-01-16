type Nullable<T> = T | null | undefined
export declare function simple<T>(x: T): T;
export declare function second<A, B>(a: A, b: B): B;
export declare function simpleWithConstraint<T extends number>(x: T): T;
export declare function complexConstraint<A extends not.exported.Foo<bigint> & not.exported.Bar, B extends typeof not.exported.Baz>(x: A): B;
declare namespace not.exported {
    interface Foo<T extends NonNullable<unknown>> {
        readonly foo: T;
    }
}
declare namespace not.exported {
    interface Bar {
        readonly bar: string;
    }
}
declare namespace not.exported {
    const Baz: {
        get baz(): boolean;
    };
}
