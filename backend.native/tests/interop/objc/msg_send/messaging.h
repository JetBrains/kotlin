#import <objc/NSObject.h>

@interface PrimitiveTestSubject : NSObject

+ (int)intFn;
+ (float)floatFn;
+ (double)doubleFn;

@end;

typedef struct {
    float f;
} SingleFloat;

typedef struct __attribute__((packed)) {
    char f1;
    short f2;
    char f3;
    char f4;
} SimplePacked;

typedef struct __attribute__((packed)) {
    char x;
    short y;
    char z;
} EvenSmallerPacked;

typedef struct {
    float f1;
    float f2;
    float f3;
    float f4;
} HomogeneousSmall;

typedef struct {
    float f1;
    float f2;
    float f3;
    float f4;
    float f5;
    float f6;
    float f7;
    float f8;
} HomogeneousBig;

// TODO: Add more cases later: SIMD, bitfields.

@interface AggregateTestSubject : NSObject

+ (SingleFloat)singleFloatFn;
+ (SimplePacked)simplePackedFn;
+ (EvenSmallerPacked)evenSmallerPackedFn;
+ (HomogeneousSmall)homogeneousSmallFn;
+ (HomogeneousBig)homogeneousBigFn;

@end;