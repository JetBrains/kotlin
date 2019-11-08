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

@protocol ObjectFactory
@required
-(id)create;
@end;

id createObjectWithFactory(id<ObjectFactory> factory) {
  return [factory create];
}

@protocol BlockProvider
@required
-(int (^)(int)) block;
@end;

int callProvidedBlock(id<BlockProvider> blockProvider, int argument) {
  return [blockProvider block](argument);
}

@protocol BlockConsumer
@required
-(int)callBlock:(int (^)(int))block argument:(int)argument;
@end;

int callPlusOneBlock(id<BlockConsumer> blockConsumer, int argument) {
  return [blockConsumer callBlock:^int(int p) { return p + 1; } argument:argument];
}

@protocol CustomRetainMethods
@required
-(id)returnRetained:(id)obj __attribute__((ns_returns_retained));
-(void)consume:(id) __attribute__((ns_consumed)) obj;
-(void)consumeSelf __attribute__((ns_consumes_self));
-(void (^)(void))returnRetainedBlock:(void (^)(void))block __attribute__((ns_returns_retained));
@end;

extern BOOL unexpectedDeallocation;

@interface MustNotBeDeallocated : NSObject
@end;

@interface CustomRetainMethodsImpl : MustNotBeDeallocated <CustomRetainMethods>
@end;

static MustNotBeDeallocated* retainedObj;
static void (^retainedBlock)(void);

void useCustomRetainMethods(id<CustomRetainMethods> p) {
  MustNotBeDeallocated* obj = [MustNotBeDeallocated new];
  retainedObj = obj; // Retain to detect possible over-release.
  [p returnRetained:obj];

  [p consume:p];
  [p consumeSelf];

  MustNotBeDeallocated* capturedObj = [MustNotBeDeallocated new];
  retainedBlock = ^{ [capturedObj description]; }; // Retain to detect possible over-release.
  [p returnRetainedBlock:retainedBlock]();
}

NSObject* createNSObject() {
  return [NSObject new];
}

@protocol ExceptionThrower
-(void)throwException;
@end;

@interface ExceptionThrowerManager : NSObject
+(void)throwExceptionWith:(id<ExceptionThrower>)thrower;
@end;

@interface Blocks : NSObject
+(BOOL)blockIsNull:(void (^)(void))block;
+(int (^)(int, int, int, int))same:(int (^)(int, int, int, int))block;

@property (class) void (^nullBlock)(void);
@property (class) void (^notNullBlock)(void);
@end;

@interface TestVarargs : NSObject
-(instancetype _Nonnull)initWithFormat:(NSString*)format, ...;
+(instancetype _Nonnull)testVarargsWithFormat:(NSString*)format, ...;
@property NSString* formatted;

+(NSString* _Nonnull)stringWithFormat:(NSString*)format, ...;
+(NSObject* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args;
@end;

@interface TestVarargs (TestVarargsExtension)
-(instancetype _Nonnull)initWithFormat:(NSString*)format, ...;
@end;

@interface TestVarargsSubclass : TestVarargs
// Test clashes:
-(instancetype _Nonnull)initWithFormat:(NSString*)format args:(void*)args;
+(NSString* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args;
@end;

@interface TestOverrideInit : NSObject
-(instancetype)initWithValue:(int)value NS_DESIGNATED_INITIALIZER;
+(instancetype)createWithValue:(int)value;
@end;

@interface MultipleInheritanceClashBase : NSObject
@property (nonnull) MultipleInheritanceClashBase* delegate;
@end;

@protocol MultipleInheritanceClash
@optional
@property (nullable) id<MultipleInheritanceClash> delegate;
@end;

@interface MultipleInheritanceClash1 : MultipleInheritanceClashBase <MultipleInheritanceClash>
@end;

@interface MultipleInheritanceClash2 : MultipleInheritanceClashBase <MultipleInheritanceClash>
@property MultipleInheritanceClashBase* delegate;
@end;

@interface TestClashingWithAny1 : NSObject
-(NSString*)toString;
-(NSString*)toString_;
-(int)hashCode;
-(BOOL)equals:(id _Nullable)other;
@end;

@interface TestClashingWithAny2 : NSObject
-(void)toString;
-(void)hashCode;
-(void)equals:(int)p; // May clash.
@end;

@interface TestClashingWithAny3 : NSObject
// Not clashing actually.
-(NSString*)toString:(int)p;
-(int)hashCode:(int)p;
-(BOOL)equals;
@end;

id getPrinterProtocolRaw() {
  return @protocol(Printer);
}

Protocol* getPrinterProtocol() {
  return @protocol(Printer);
}

@interface TestInitWithCustomSelector : NSObject
-(instancetype)initCustom;
@property BOOL custom;

+(instancetype _Nonnull)createCustom;
@end;

@interface TestAllocNoRetain : NSObject
@property BOOL ok;
@end;

@interface CustomString : NSString
- (instancetype)initWithValue:(int)value;
@property int value;
@end;

CustomString* _Nonnull createCustomString(int value) {
    return [[CustomString alloc] initWithValue:value];
}

int getCustomStringValue(CustomString* str) {
    return str.value;
}

extern BOOL customStringDeallocated;
