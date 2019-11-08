#import <stdio.h>
#import <Foundation/NSString.h>
#import "smoke.h"

@interface CPrinter : NSObject <Printer>
-(void)print:(const char*)string;
@end;

@implementation CPrinter
-(void)print:(const char*)string {
    printf("%s\n", string);
    fflush(stdout);
}
@end;

@implementation Foo

@synthesize name;

-(instancetype)init {
    if (self = [super init]) {
        self.name = @"World";
    }
    return self;
}

-(void)helloWithPrinter:(id <Printer>)printer {
    NSString* message = [NSString stringWithFormat:@"Hello, %@!", self.name];
    [printer print:message.UTF8String];
}

-(void)dealloc {
    printf("Deallocated\n");
}

@end;

@implementation Foo (FooExtensions)

-(void)hello {
    CPrinter* printer = [[CPrinter alloc] init];
    [self helloWithPrinter:printer];
}
@end;

void replacePairElements(id <MutablePair> pair, int first, int second) {
    [pair update:0 add:(first - pair.first)];
    [pair update:1 sub:(pair.second - second)];
}

int (^getSupplier(int x))(void) {
    return ^{
        return x;
    };
}

Class (^ _Nonnull getClassGetter(NSObject* obj))() {
    return ^{ return obj.class; };
}

NSString* globalString = @"Global string";
NSObject* globalObject = nil;

int formatStringLength(NSString* format, ...) {
  va_list args;
  va_start(args, format);
  NSString* result = [[NSString alloc] initWithFormat:format arguments:args];
  va_end(args);
  return result.length;
}

BOOL unexpectedDeallocation = NO;

@implementation MustNotBeDeallocated
-(void)dealloc {
  unexpectedDeallocation = YES;
}
@end;

static CustomRetainMethodsImpl* retainedCustomRetainMethodsImpl;

@implementation CustomRetainMethodsImpl
-(id)returnRetained:(id)obj __attribute__((ns_returns_retained)) {
    return obj;
}

-(void)consume:(id) __attribute__((ns_consumed)) obj {
}

-(void)consumeSelf __attribute__((ns_consumes_self)) {
  retainedCustomRetainMethodsImpl = self; // Retain to detect possible over-release.
}

-(void (^)(void))returnRetainedBlock:(void (^)(void))block __attribute__((ns_returns_retained)) {
    return block;
}
@end;

@implementation ExceptionThrowerManager
+(void)throwExceptionWith:(id<ExceptionThrower>)thrower {
    [thrower throwException];
}
@end;

@implementation Blocks
+(BOOL)blockIsNull:(void (^)(void))block {
    return block == nil;
}

+(int (^)(int, int, int, int))same:(int (^)(int, int, int, int))block {
    return block;
}

+(void (^)(void)) nullBlock {
    return nil;
}

+(void (^)(void)) notNullBlock {
    return ^{};
}

@end;

@implementation TestVarargs
-(instancetype _Nonnull)initWithFormat:(NSString*)format, ... {
    self = [super init];

    va_list args;
    va_start(args, format);
    self.formatted = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);

    return self;
}

+(instancetype _Nonnull)testVarargsWithFormat:(NSString*)format, ... {
    TestVarargs* result = [[self alloc] init];

    va_list args;
    va_start(args, format);
    result.formatted = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);

    return result;
}

+(NSString* _Nonnull)stringWithFormat:(NSString*)format, ... {
    va_list args;
    va_start(args, format);
    NSString* result = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);

    return result;
}

+(NSObject* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args {
    abort();
}

@end;

@implementation TestVarargsSubclass
-(instancetype _Nonnull)initWithFormat:(NSString*)format args:(void*)args {
    abort();
}

+(NSString* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args {
    abort();
}
@end;

@implementation TestOverrideInit
-(instancetype)initWithValue:(int)value {
    return self = [super init];
}

+(instancetype)createWithValue:(int)value {
    return [[self alloc] initWithValue:value];
}
@end;

@implementation MultipleInheritanceClashBase
@end;

@implementation MultipleInheritanceClash1
@end;

@implementation MultipleInheritanceClash2
@end;

@implementation TestClashingWithAny1
-(NSString*)description {
    return @"description";
}

-(NSString*)toString {
    return @"toString";
}

-(NSString*)toString_ {
    return @"toString_";
}

-(NSUInteger)hash {
    return 1;
}

-(int)hashCode {
    return 31;
}

-(BOOL)equals:(id _Nullable)other {
    return YES;
}
@end;

@implementation TestClashingWithAny2
-(NSString*)description {
    return @"description";
}

-(void)toString {
}

-(NSUInteger)hash {
    return 2;
}

-(void)hashCode {
}

-(void)equals:(int)p {
}
@end;

@implementation TestClashingWithAny3
-(NSString*)description {
    return @"description";
}

-(NSString*)toString:(int)p {
    return [NSString stringWithFormat:@"%s:%d", "toString", p];
}

-(NSUInteger)hash {
    return 3;
}

-(int)hashCode:(int)p {
    return p + 1;
}

-(BOOL)equals {
    return YES;
}
@end;

@implementation TestInitWithCustomSelector

-(instancetype)initCustom {
    if (self = [super init]) {
        self.custom = YES;
    }
    return self;
}

-(instancetype)init {
    if (self = [super init]) {
        self.custom = NO;
    }
    return self;
}

+(instancetype)createCustom {
    return [[self alloc] initCustom];
}

@end;

@implementation TestAllocNoRetain
-(instancetype)init {
    __weak id weakSelf = self;
    self = [TestAllocNoRetain alloc];
    if (self = [super init]) {
        // Ensure that original self value was deallocated:
        self.ok = (weakSelf == nil);
        // So it's RC was 1, which means there wasn't redundant retain applied to it.
    }
    return self;
}
@end;

BOOL customStringDeallocated = NO;

@implementation CustomString {
    NSString* delegate;
}

- (instancetype)initWithValue:(int)value {
    if (self = [super init]) {
        self->delegate = @(value).description;
        self.value = value;
    }
    return self;
}

- (unichar)characterAtIndex:(NSUInteger)index {
    return [self->delegate characterAtIndex:index];
}

- (NSUInteger)length {
    return self->delegate.length;
}

- (id)copyWithZone:(NSZone *)zone {
    return self;
}

- (void)dealloc {
    customStringDeallocated = YES;
}
@end;
