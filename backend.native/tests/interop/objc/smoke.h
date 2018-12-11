#import <objc/NSObject.h>
#import <CoreFoundation/CoreFoundation.h>

@class Foo;

@protocol Printer;
@protocol Printer;

@protocol Empty
@end;

@protocol Forward;
@class Forward;

void useForward1(Forward * p) {}
void useForward2(id<Forward> p) {}

typedef NSString NSStringTypedef;

@interface Foo : NSObject <Empty>
@property NSStringTypedef* name;
-(void)helloWithPrinter:(id <Printer>)printer;
@end;

@interface Foo (FooExtensions)
-(void)hello;
@end;

@protocol Printer
@required
-(void)print:(const char*)string;
@end;

@protocol MutablePair
@required
@property (readonly) int first;
@property (readonly) int second;

-(void)update:(int)index add:(int)delta;
-(void)update:(int)index sub:(int)delta;

@end;

void replacePairElements(id <MutablePair> pair, int first, int second);

int invoke1(int arg, int (^block)(int)) {
    return block(arg);
}

void invoke2(void (^block)(void)) {
    block();
}

int (^getSupplier(int x))(void);
Class (^ _Nonnull getClassGetter(NSObject* obj))(void);

extern NSString* globalString;
extern NSObject* globalObject;

int formatStringLength(NSString* format, ...);

#define STRING_MACRO @"String macro"
#define CFSTRING_MACRO CFSTR("CFString macro")

typedef NS_ENUM(int32_t, ForwardDeclaredEnum);
typedef NS_ENUM(int32_t, ForwardDeclaredEnum) {
    ZERO, ONE, TWO,
};