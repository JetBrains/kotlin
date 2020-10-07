#import "kt41811.h"
#import "assert.h"

id retainObject = nil;
BOOL deallocRetainReleaseDeallocated = NO;

@implementation DeallocRetainRelease
-(void)dealloc {
    retainObject = self;
    assert(retainObject == self);
    retainObject = nil;

    assert(!deallocRetainReleaseDeallocated);
    deallocRetainReleaseDeallocated = YES;
}
@end;

@implementation ObjCWeakReference
@end;

id <WeakReference> weakDeallocLoadWeak = nil;
BOOL deallocLoadWeakDeallocated = NO;

@implementation DeallocLoadWeak
-(void)checkWeak {
    assert(weakDeallocLoadWeak != nil);
    assert(weakDeallocLoadWeak.referent == self);
}

-(void)dealloc {
    assert(weakDeallocLoadWeak != nil);
    assert(weakDeallocLoadWeak.referent == nil);

    assert(!deallocLoadWeakDeallocated);
    deallocLoadWeakDeallocated = YES;
}
@end;
