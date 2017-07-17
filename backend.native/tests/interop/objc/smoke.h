#import <objc/NSObject.h>

@protocol Printer
@required
-(void)print:(const char*)string;
@end;

@interface Foo : NSObject
@property NSString* name;
-(void)hello;
-(void)helloWithPrinter:(id <Printer>)printer;
@end;

@protocol MutablePair
@required
@property (readonly) int first;
@property (readonly) int second;

-(void)update:(int)index add:(int)delta;
-(void)update:(int)index sub:(int)delta;

@end;

void replacePairElements(id <MutablePair> pair, int first, int second);
