//
//  ViewController.m
//  Hello
//
//  Created by jetbrains on 14/10/16.
//  Copyright © 2016 jetbrains. All rights reserved.
//

#import "ViewController.h"
#import "ffi.h"

@interface ViewController ()

@property (strong, nonatomic) IBOutlet UILabel *helloLabel;

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    UILabel *label = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, 200, 50)];
    label.text = @"Initialized!";
    self.helloLabel = label;
    [self.view addSubview:label];
   
    [self addMyButton];
}


- (void)addMyButton{    // Method for creating button, with background image and other properties
    UIButton *playButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    playButton.frame = CGRectMake(110.0, 160.0, 100.0, 30.0);
    [playButton setTitle:@"Start" forState:UIControlStateNormal];
    [playButton addTarget:self action:@selector(playAction:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:playButton];
}

/* Acts like puts with the file given at time of enclosure. */
void puts_binding(ffi_cif *cif, void *ret, void* args[],
                  void *stream)
{
    *(ffi_arg *)ret = fputs(*(char **)args[0], (FILE *)stream);
}

typedef int (*puts_t)(char *);

int test()
{
    ffi_cif cif;
    ffi_type *args[1];
    ffi_closure *closure;
    
    void *bound_puts;
    int rc;
    
    /* Allocate closure and bound_puts */
    closure = ffi_closure_alloc(sizeof(ffi_closure), &bound_puts);
    
    if (closure)
    {
        /* Initialize the argument info vectors */
        args[0] = &ffi_type_pointer;
        
        /* Initialize the cif */
        if (ffi_prep_cif(&cif, FFI_DEFAULT_ABI, 1,
                         &ffi_type_sint, args) == FFI_OK)
        {
            /* Initialize the closure, setting stream to stdout */
            if (ffi_prep_closure_loc(closure, &cif, puts_binding,
                                     stdout, bound_puts) == FFI_OK)
            {
                rc = ((puts_t)bound_puts)("Hello World!");
                /* rc now holds the result of the call to fputs */
            }
        }
    }
    
    /* Deallocate both closure, and bound_puts */
    ffi_closure_free(closure);
    
    return 0;
}

-(void) playAction:(UIButton*)sender {
    self.helloLabel.text = @"Running...";
    test();
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end
