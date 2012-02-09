{
    var classes = function () {
        var Vector = Kotlin.Class.create({initialize:function (x, y) {
            this.$x = x;
            this.$y = y;
        }, get_x:function () {
            return this.$x;
        }, set_x:function (tmp$0) {
            this.$x = tmp$0;
        }, get_y:function () {
            return this.$y;
        }, set_y:function (tmp$0) {
            this.$y = tmp$0;
        }, plus:function (v) {
            {
                return interactive.v_0(this.get_x() + v.get_x(), this.get_y() + v.get_y());
            }
        }, minus:function (v) {
            {
                return interactive.v_0(this.get_x() - v.get_x(), this.get_y() - v.get_y());
            }
        }, times:function (coef) {
            {
                return interactive.v_0(this.get_x() * coef, this.get_y() * coef);
            }
        }, distanceTo:function (v) {
            {
                return Math.sqrt(this.minus(v).get_sqr());
            }
        }, isInRect:function (topLeft, size) {
            {
                return this.get_x() >= topLeft.get_x() && this.get_x() <= topLeft.get_x() + size.get_x() && this.get_y() >= topLeft.get_y() && this.get_y() <= topLeft.get_y() + size.get_y();
            }
        }, get_sqr:function () {
            {
                return this.get_x() * this.get_x() + this.get_y() * this.get_y();
            }
        }
        });
        var CanvasState = Kotlin.Class.create({initialize:function (canvas) {
            this.$canvas = canvas;
            this.$width = this.get_canvas().width;
            this.$height = this.get_canvas().height;
            this.$context = getContext();
            this.$valid = false;
            this.$shapes = new Kotlin.ArrayList;
            this.$selection = null;
            this.$dragOff = new interactive.Vector_0(0, 0);
            this.$interval = 1000 / 30;
            this.$size = 20;
            {
                var tmp$4;
                var tmp$3;
                var tmp$2;
                var tmp$1;
                var tmp$0_0;
                $(this.get_canvas()).mousedown((tmp$0_0 = this , function (it) {
                    {
                        var tmp$0;
                        tmp$0_0.set_valid(false);
                        tmp$0_0.set_selection(null);
                        var mousePos = tmp$0_0.mousePos_0(it);
                        {
                            tmp$0 = tmp$0_0.get_shapes().iterator();
                            while (tmp$0.hasNext()) {
                                var shape = tmp$0.next();
                                {
                                    if (shape.contains(mousePos)) {
                                        tmp$0_0.set_dragOff(mousePos.minus(shape.get_pos()));
                                        shape.set_selected(true);
                                        tmp$0_0.set_selection(shape);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                    ));
                $(this.get_canvas()).mousemove((tmp$1 = this , function (it) {
                    {
                        if (tmp$1.get_selection() != null) {
                            Kotlin.sure(tmp$1.get_selection()).set_pos(tmp$1.mousePos_0(it).minus(tmp$1.get_dragOff()));
                            tmp$1.set_valid(false);
                        }
                    }
                }
                    ));
                $(this.get_canvas()).mouseup((tmp$2 = this , function (it) {
                    {
                        if (tmp$2.get_selection() != null) {
                            Kotlin.sure(tmp$2.get_selection()).set_selected(false);
                        }
                        tmp$2.set_selection(null);
                        tmp$2.set_valid(false);
                    }
                }
                    ));
                $(this.get_canvas()).dblclick((tmp$3 = this , function (it) {
                    {
                        var redTransparentCircle = new interactive.Circle_0(tmp$3.mousePos_0(it), tmp$3.get_size(), 'rgba(200, 100, 100, 0.3)');
                        tmp$3.addShape(redTransparentCircle);
                        tmp$3.set_valid(false);
                    }
                }
                    ));
                interactive.doWithPeriod(this.get_interval(), (tmp$4 = this , function () {
                    {
                        tmp$4.draw();
                    }
                }
                    ));
            }
        }, get_canvas:function () {
            return this.$canvas;
        }, get_width:function () {
            return this.$width;
        }, get_height:function () {
            return this.$height;
        }, get_context:function () {
            return this.$context;
        }, get_valid:function () {
            return this.$valid;
        }, set_valid:function (tmp$0) {
            this.$valid = tmp$0;
        }, get_shapes:function () {
            return this.$shapes;
        }, set_shapes:function (tmp$0) {
            this.$shapes = tmp$0;
        }, get_selection:function () {
            return this.$selection;
        }, set_selection:function (tmp$0) {
            this.$selection = tmp$0;
        }, get_dragOff:function () {
            return this.$dragOff;
        }, set_dragOff:function (tmp$0) {
            this.$dragOff = tmp$0;
        }, get_interval:function () {
            return this.$interval;
        }, get_size:function () {
            return this.$size;
        }, mousePos_0:function (e) {
            {
                var offset = new interactive.Vector_0(0, 0);
                var element = this.get_canvas();
                while (element != null) {
                    var el = Kotlin.sure(element);
                    offset = offset.plus(new interactive.Vector_0(el.offsetLeft, el.offsetTop));
                    element = el.offsetParent;
                }
                return (new interactive.Vector_0(e.pageX, e.pageY)).minus(offset);
            }
        }, addShape:function (shape) {
            {
                this.get_shapes().add(shape);
                this.set_valid(false);
            }
        }, clear:function () {
            {
                this.get_context().fillStyle = '#FFFFFF';
                this.get_context().fillRect(0, 0, this.get_width(), this.get_height());
                this.get_context().strokeStyle = '#000000';
                this.get_context().lineWidth = 4;
                this.get_context().strokeRect(0, 0, this.get_width(), this.get_height());
            }
        }, draw:function () {
            {
                var tmp$0;
                if (this.get_valid())
                    return;
                this.clear();
                {
                    tmp$0 = this.get_shapes().iterator();
                    while (tmp$0.hasNext()) {
                        var shape = tmp$0.next();
                        {
                            shape.draw(this);
                        }
                    }
                }
                this.set_valid(true);
            }
        }
        });
        var Shape = Kotlin.Class.create({initialize:function () {
            this.$selected = false;
        }, draw:function (state) {
        }, contains:function (mousePos) {
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_selected:function () {
            return this.$selected;
        }, set_selected:function (tmp$0) {
            this.$selected = tmp$0;
        }
        });
        var JB = Kotlin.Class.create(Shape, {initialize:function (pos, relSize) {
            this.$pos = pos;
            this.$relSize = relSize;
            this.super_init();
            this.$imageSize = interactive.v_0(704, 254);
            this.$canvasSize = this.get_imageSize().times(this.get_relSize());
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_relSize:function () {
            return this.$relSize;
        }, set_relSize:function (tmp$0) {
            this.$relSize = tmp$0;
        }, get_imageSize:function () {
            return this.$imageSize;
        }, get_canvasSize:function () {
            return this.$canvasSize;
        }, draw:function (state) {
            {
                var context = state.get_context();
                context.drawImage(getKotlinLogo(), 0, 0, this.get_imageSize().get_x(), this.get_imageSize().get_y(), this.get_pos().get_x(), this.get_pos().get_y(), this.get_canvasSize().get_x(), this.get_canvasSize().get_y());
            }
        }, contains:function (mousePos) {
            {
                return mousePos.isInRect(this.get_pos(), this.get_canvasSize());
            }
        }
        });
        var Rectangle = Kotlin.Class.create(Shape, {initialize:function (pos, size) {
            this.$pos = pos;
            this.$size = size;
            this.super_init();
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_size:function () {
            return this.$size;
        }, set_size:function (tmp$0) {
            this.$size = tmp$0;
        }, draw:function (state) {
            {
                var context = state.get_context();
                context.fillStyle = 'rgba(0,255,0,.6)';
                context.fillRect(this.get_pos().get_x(), this.get_pos().get_y(), this.get_size().get_x(), this.get_size().get_y());
                if (this.get_selected()) {
                    context.strokeStyle = '#FF0000';
                    context.lineWidth = 2;
                    context.strokeRect(this.get_pos().get_x(), this.get_pos().get_y(), this.get_size().get_x(), this.get_size().get_y());
                }
            }
        }, contains:function (mousePos) {
            {
                return mousePos.isInRect(this.get_pos(), this.get_size());
            }
        }
        });
        var Circle = Kotlin.Class.create(Shape, {initialize:function (pos, radius, fillColor) {
            this.$pos = pos;
            this.$radius = radius;
            this.$fillColor = fillColor;
            this.super_init();
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_radius:function () {
            return this.$radius;
        }, set_radius:function (tmp$0) {
            this.$radius = tmp$0;
        }, get_fillColor:function () {
            return this.$fillColor;
        }, set_fillColor:function (tmp$0) {
            this.$fillColor = tmp$0;
        }, draw:function (state) {
            {
                var context = state.get_context();
                context.shadowColor = 'white';
                context.shadowBlur = 10;
                context.fillStyle = this.get_fillColor();
                context.beginPath();
                context.arc(this.get_pos().get_x(), this.get_pos().get_y(), this.get_radius(), 0, 2 * Math.PI, false);
                context.closePath();
                context.fill();
            }
        }, contains:function (mousePos) {
            {
                return this.get_pos().distanceTo(mousePos) < this.get_radius();
            }
        }
        });
        return {Shape_0:Shape, JB_0:JB, Rectangle_0:Rectangle, Circle_0:Circle, CanvasState_0:CanvasState, Vector_0:Vector};
    }
        ();
    var interactive = Kotlin.Namespace.create({initialize:function () {
        interactive.$state = new interactive.CanvasState_0(getCanvas());
    }, get_state:function () {
        return interactive.$state;
    }, doWithPeriod:function (period, f) {
        {
            setInterval(f, period);
        }
    }, v_0:function (x, y) {
        {
            return new interactive.Vector_0(x, y);
        }
    }, main:function () {
        {
            var state = new interactive.CanvasState_0(getCanvas());
            state.addShape(new interactive.JB_0(interactive.v_0(1, 1), 0.3));
            setTimeout(function () {
                    {
                        state.set_valid(false);
                    }
                }
            );
        }
    }
    }, classes);
    interactive.initialize();
}

var args = [];
interactive.main(args);
