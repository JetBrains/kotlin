type Nullable<T> = T | null | undefined
export declare function getResult(): not.exported.org.second.Result<string>;
declare namespace not.exported.org.second {
    class Result<T extends NonNullable<unknown>> extends not.exported.org.second.BaseResult<T> {
        constructor();
    }
}
declare namespace not.exported.org.second {
    abstract class BaseResult<T extends NonNullable<unknown>> {
        constructor(foo: typeof not.exported.org.second.Foo);
    }
}
declare namespace not.exported.org.second {
    const Foo: {
        get bar(): not.exported.Parent.OneMoreLayer.MentionedNested;
        get baz(): string;
        get oneMore(): not.exported.Parent.Companion.AnotherMentionedNested;
    } & not.exported.Baz<string>;
}
declare namespace not.exported {
    interface Baz<T> extends not.exported.Bar {
        readonly baz?: T;
        readonly bar: not.exported.Parent.OneMoreLayer.MentionedNested;
        readonly oneMore: not.exported.Parent.Companion.AnotherMentionedNested;
    }
}
declare namespace not.exported.Parent.OneMoreLayer {
    interface MentionedNested {
        readonly value: typeof not.exported.MentionedParent;
    }
}
declare namespace not.exported.Parent.Companion {
    class AnotherMentionedNested {
        constructor();
        get value(): string;
    }
}
declare namespace not.exported {
    interface Bar {
        readonly bar: not.exported.Parent.OneMoreLayer.MentionedNested;
        readonly oneMore: not.exported.Parent.Companion.AnotherMentionedNested;
    }
}
declare namespace not.exported {
    const MentionedParent: {
        get value(): string;
    };
}