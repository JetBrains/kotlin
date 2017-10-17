#import <objc/NSObject.h>

@protocol Printer
@required
-(void)print:(const char*)string;
@end;

typedef NSString NSStringTypedef;

@interface Foo : NSObject
@property NSStringTypedef* name;
-(void)helloWithPrinter:(id <Printer>)printer;
@end;

@interface Foo (FooExtensions)
-(void)hello;
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
