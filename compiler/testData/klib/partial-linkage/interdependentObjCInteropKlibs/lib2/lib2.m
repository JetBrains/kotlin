#import "lib2.h"

@implementation ClassWithUnmodifiedProtocol
+ (int)staticMethod:(int)value {
    return value;
}
@end

@implementation ClassWithRemovedProtocol
+ (int)staticMethod:(int)value {
    return value;
}
@end

@implementation ClassWithDuplicatedProtocol
@end

@implementation ClassWithBaseClassToProtocol
@end

@implementation RemovedClassWrapper
@end
