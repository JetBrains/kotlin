type Nullable<T> = T | null | undefined
export declare function produceBoolean(): boolean;
export declare function produceNumber(): number;
export declare function produceBigInt(): bigint;
export declare function produceString(): string;
export declare function produceAny(): NonNullable<unknown>;
export declare function consumeBoolean(x: boolean): string;
export declare function consumeNumber(x: number): string;
export declare function consumeBigInt(x: bigint): string;
export declare function consumeString(x: string): string;
export declare function consumeAny(x: NonNullable<unknown>): string;